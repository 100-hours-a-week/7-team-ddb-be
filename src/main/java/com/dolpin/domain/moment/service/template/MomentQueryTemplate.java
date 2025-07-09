package com.dolpin.domain.moment.service.template;

import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.MomentViewService;
import com.dolpin.domain.moment.service.cache.MomentCacheService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class MomentQueryTemplate {

    protected final MomentRepository momentRepository;
    protected final UserQueryService userQueryService;
    protected final CommentRepository commentRepository;
    protected final MomentViewService momentViewService;
    protected final MomentCacheService momentCacheService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @Transactional(readOnly = true)
    public MomentListResponse executeMomentQuery(MomentQueryContext context) {
        log.debug("Moment 조회 시작: queryType={}, userId={}, limit={}",
                context.getQueryType(), context.getCurrentUserId(), context.getLimit());

        // 1. 사전 검증 (각 구현체에서 정의)
        validateBeforeQuery(context);

        // 2. 파라미터 처리
        int pageSize = validateAndGetLimit(context.getLimit());
        int queryLimit = pageSize + 1;

        // 3. 데이터 조회 (각 구현체에서 정의)
        List<Moment> moments = fetchMoments(context, queryLimit);

        // 4. 응답 빌드 (공통 로직)
        return buildMomentListResponse(
                moments,
                pageSize,
                shouldIncludeAuthor(context),
                context.getCursor(),
                generateBaseUrl(context)
        );
    }

    protected abstract List<Moment> fetchMoments(MomentQueryContext context, int queryLimit);

    protected abstract String generateBaseUrl(MomentQueryContext context);

    protected void validateBeforeQuery(MomentQueryContext context) {
        // 기본 구현: 아무것도 하지 않음
    }

    protected boolean shouldIncludeAuthor(MomentQueryContext context) {
        return context.getQueryType() == MomentQueryType.ALL_MOMENTS ||
                context.getQueryType() == MomentQueryType.PLACE_MOMENTS;
    }

    private int validateAndGetLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("limit은 0보다 커야 합니다."));
        }
        if (limit > MAX_LIMIT) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("limit은 " + MAX_LIMIT + " 이하여야 합니다."));
        }
        return limit;
    }

    private MomentListResponse buildMomentListResponse(List<Moment> moments, int pageSize,
                                                       boolean includeAuthor, String currentCursor, String baseUrl) {
        boolean hasNext = moments.size() > pageSize;
        List<Moment> actualMoments = hasNext ? moments.subList(0, pageSize) : moments;

        if (actualMoments.isEmpty()) {
            return buildEmptyMomentListResponse(pageSize, baseUrl);
        }

        String nextCursor = hasNext ? generateCursor(moments.get(pageSize)) : null;

        // 댓글 수 조회 (캐시 우선)
        Map<Long, Long> commentCountMap = getCommentCountMapWithCache(actualMoments);

        // 조회 수 조회 (캐시 우선)
        Map<Long, Long> viewCountMap = getViewCountMapWithCache(actualMoments);

        // Author 정보가 필요한 경우 조회
        Map<Long, User> authorMap = new HashMap<>();
        if (includeAuthor && !actualMoments.isEmpty()) {
            List<Long> userIds = actualMoments.stream()
                    .map(Moment::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            authorMap = userIds.stream()
                    .collect(Collectors.toMap(
                            userId -> userId,
                            userQueryService::getUserById
                    ));
        }

        // final 변수로 선언하여 lambda에서 사용 가능하게 함
        final Map<Long, Long> finalCommentCountMap = commentCountMap;
        final Map<Long, Long> finalViewCountMap = viewCountMap;
        final Map<Long, User> finalAuthorMap = authorMap;

        // MomentSummaryDto 변환
        List<MomentListResponse.MomentSummaryDto> momentSummaries = actualMoments.stream()
                .map(moment -> buildMomentSummaryDto(
                        moment,
                        includeAuthor,
                        finalCommentCountMap,
                        finalViewCountMap,
                        finalAuthorMap
                ))
                .collect(Collectors.toList());

        // Meta 정보 생성
        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(pageSize)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();

        // Links 정보 생성
        MomentListResponse.LinksDto links = MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder()
                        .href(baseUrl + buildQueryParams(currentCursor, pageSize))
                        .build())
                .next(hasNext ? MomentListResponse.LinkDto.builder()
                        .href(baseUrl + buildQueryParams(nextCursor, pageSize))
                        .build() : null)
                .build();

        return MomentListResponse.builder()
                .moments(momentSummaries)
                .meta(meta)
                .links(links)
                .build();
    }

    private Map<Long, Long> getCommentCountMapWithCache(List<Moment> moments) {
        List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());

        if (momentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 1. 캐시에서 댓글 수 조회
        Map<Long, Long> cachedCommentCounts = momentCacheService.getCommentCounts(momentIds);

        // 2. 캐시에 없는 기록 ID 찾기
        List<Long> missedMomentIds = momentIds.stream()
                .filter(momentId -> !cachedCommentCounts.containsKey(momentId))
                .collect(Collectors.toList());

        Map<Long, Long> result = new HashMap<>(cachedCommentCounts);

        // 3. 캐시 미스 데이터는 DB에서 조회
        if (!missedMomentIds.isEmpty()) {
            List<Object[]> commentCountResults = commentRepository.countByMomentIds(missedMomentIds);
            Map<Long, Long> dbCommentCounts = commentCountResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0],    // momentId
                            row -> (Long) row[1]     // commentCount
                    ));

            // 4. 결과에 추가
            result.putAll(dbCommentCounts);

            // 5. 조회된 데이터를 캐시에 저장 (비동기)
            momentCacheService.cacheCommentCountsBatch(dbCommentCounts);
        }

        return result;
    }

    private Map<Long, Long> getViewCountMapWithCache(List<Moment> moments) {
        List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());

        if (momentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 1. 캐시에서 조회 수 조회
        Map<Long, Long> cachedViewCounts = momentCacheService.getViewCounts(momentIds);

        // 2. 캐시에 없는 기록 ID 찾기
        List<Long> missedMomentIds = momentIds.stream()
                .filter(momentId -> !cachedViewCounts.containsKey(momentId))
                .collect(Collectors.toList());

        Map<Long, Long> result = new HashMap<>(cachedViewCounts);

        // 3. 캐시 미스 데이터는 엔티티에서 가져오기
        if (!missedMomentIds.isEmpty()) {
            Map<Long, Long> dbViewCounts = new HashMap<>();
            for (Moment moment : moments) {
                if (missedMomentIds.contains(moment.getId())) {
                    dbViewCounts.put(moment.getId(), moment.getViewCount());
                }
            }

            // 4. 결과에 추가
            result.putAll(dbViewCounts);

            // 5. 조회된 데이터를 캐시에 저장 (비동기)
            momentCacheService.cacheViewCountsBatch(dbViewCounts);
        }

        return result;
    }

    private MomentListResponse.MomentSummaryDto buildMomentSummaryDto(Moment moment, boolean includeAuthor,
                                                                      Map<Long, Long> commentCountMap,
                                                                      Map<Long, Long> viewCountMap,
                                                                      Map<Long, User> authorMap) {
        String thumbnail = moment.getThumbnailUrl();

        MomentListResponse.MomentSummaryDto.MomentSummaryDtoBuilder builder = MomentListResponse.MomentSummaryDto.builder()
                .id(moment.getId())
                .title(moment.getTitle())
                .content(moment.getContent())
                .thumbnail(thumbnail)
                .imagesCount(moment.getImageCount())
                .isPublic(moment.getIsPublic())
                .createdAt(moment.getCreatedAt())
                .commentCount(commentCountMap.getOrDefault(moment.getId(), 0L))
                .viewCount(viewCountMap.getOrDefault(moment.getId(), moment.getViewCount()));

        if (includeAuthor) {
            User author = authorMap.get(moment.getUserId());
            if (author != null) {
                builder.author(MomentListResponse.AuthorDto.builder()
                        .id(author.getId())
                        .nickname(author.getUsername())
                        .profileImage(author.getImageUrl())
                        .build());
            }
        }

        return builder.build();
    }

    private MomentListResponse buildEmptyMomentListResponse(int pageSize, String baseUrl) {
        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(pageSize)
                        .nextCursor(null)
                        .hasNext(false)
                        .build())
                .build();

        MomentListResponse.LinksDto links = MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder()
                        .href(baseUrl + "?limit=" + pageSize)
                        .build())
                .next(null)
                .build();

        return MomentListResponse.builder()
                .moments(List.of())
                .meta(meta)
                .links(links)
                .build();
    }

    private String generateCursor(Moment moment) {
        return moment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    }

    private String buildQueryParams(String cursor, int limit) {
        StringBuilder params = new StringBuilder("?limit=" + limit);
        if (cursor != null) {
            params.append("&cursor=").append(cursor);
        }
        return params.toString();
    }
}
