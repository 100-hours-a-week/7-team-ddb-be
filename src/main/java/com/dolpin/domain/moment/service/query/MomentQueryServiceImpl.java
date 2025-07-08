package com.dolpin.domain.moment.service.query;

import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.MomentViewService;
import com.dolpin.domain.moment.service.cache.MomentCacheService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentQueryServiceImpl implements MomentQueryService {

    private final MomentRepository momentRepository;
    private final UserQueryService userQueryService;
    private final CommentRepository commentRepository;
    private final MomentViewService momentViewService;
    private final MomentCacheService momentCacheService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    // =============== Template Method 패턴 적용 ===============
    @Transactional(readOnly = true)
    protected MomentListResponse getMomentList(MomentQueryStrategy strategy, MomentQueryContext context) {
        // 1. 사전 검증 (Hook Method)
        strategy.validateBeforeQuery(context);

        // 2. 파라미터 처리
        int pageSize = validateAndGetLimit(context.getLimit());
        int queryLimit = pageSize + 1;

        // 3. 데이터 조회 (Strategy Method)
        List<Moment> moments = strategy.fetchMoments(context, queryLimit);

        // 4. 응답 빌드 (공통 로직)
        return buildMomentListResponse(
                moments,
                pageSize,
                strategy.shouldIncludeAuthor(context),
                context.getCursor(),
                strategy.generateBaseUrl(context)
        );
    }


    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getAllMoments(Long currentUserId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.builder()
                .queryType(MomentQueryType.ALL_MOMENTS)
                .currentUserId(currentUserId)
                .limit(limit)
                .cursor(cursor)
                .build();

        return getMomentList(new AllMomentsQueryStrategy(), context);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getMyMoments(Long userId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.builder()
                .queryType(MomentQueryType.MY_MOMENTS)
                .currentUserId(userId)
                .limit(limit)
                .cursor(cursor)
                .build();

        return getMomentList(new MyMomentsQueryStrategy(), context);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getUserMoments(Long targetUserId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.builder()
                .queryType(MomentQueryType.USER_MOMENTS)
                .targetUserId(targetUserId)
                .limit(limit)
                .cursor(cursor)
                .build();

        return getMomentList(new UserMomentsQueryStrategy(), context);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getPlaceMoments(Long placeId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.builder()
                .queryType(MomentQueryType.PLACE_MOMENTS)
                .placeId(placeId)
                .limit(limit)
                .cursor(cursor)
                .build();

        return getMomentList(new PlaceMomentsQueryStrategy(), context);
    }

    private interface MomentQueryStrategy {
        List<Moment> fetchMoments(MomentQueryContext context, int queryLimit);

        default void validateBeforeQuery(MomentQueryContext context) {
            // 기본 구현: 아무것도 하지 않음
        }

        default boolean shouldIncludeAuthor(MomentQueryContext context) {
            return context.getQueryType() == MomentQueryType.ALL_MOMENTS ||
                    context.getQueryType() == MomentQueryType.PLACE_MOMENTS;
        }

        default String generateBaseUrl(MomentQueryContext context) {
            return switch (context.getQueryType()) {
                case ALL_MOMENTS -> "/api/v1/users/moments";
                case MY_MOMENTS -> "/api/v1/users/me/moments";
                case USER_MOMENTS -> "/api/v1/users/" + context.getTargetUserId() + "/moments";
                case PLACE_MOMENTS -> "/api/v1/places/" + context.getPlaceId() + "/moments";
            };
        }
    }


    private class AllMomentsQueryStrategy implements MomentQueryStrategy {
        @Override
        public List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
            return momentRepository.findPublicMomentsWithUserPrivateNative(
                    context.getCurrentUserId(),
                    context.getCursor(),
                    queryLimit
            );
        }
    }

    private class MyMomentsQueryStrategy implements MomentQueryStrategy {
        @Override
        public List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
            return momentRepository.findByUserIdWithVisibilityNative(
                    context.getCurrentUserId(),
                    true,
                    context.getCursor(),
                    queryLimit
            );
        }

        @Override
        public boolean shouldIncludeAuthor(MomentQueryContext context) {
            return false; // 내 기록에는 작성자 정보 불필요
        }
    }

    private class UserMomentsQueryStrategy implements MomentQueryStrategy {
        @Override
        public void validateBeforeQuery(MomentQueryContext context) {
            userQueryService.getUserById(context.getTargetUserId());
        }

        @Override
        public List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
            return momentRepository.findByUserIdWithVisibilityNative(
                    context.getTargetUserId(),
                    false,
                    context.getCursor(),
                    queryLimit
            );
        }

        @Override
        public boolean shouldIncludeAuthor(MomentQueryContext context) {
            return false; // 특정 사용자 기록에는 작성자 정보 불필요
        }
    }

    private class PlaceMomentsQueryStrategy implements MomentQueryStrategy {
        @Override
        public List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
            return momentRepository.findPublicMomentsByPlaceIdNative(
                    context.getPlaceId(),
                    context.getCursor(),
                    queryLimit
            );
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class MomentQueryContext {
        private MomentQueryType queryType;
        private Long currentUserId;
        private Long targetUserId;
        private Long placeId;
        private Integer limit;
        private String cursor;
    }

    public enum MomentQueryType {
        ALL_MOMENTS, MY_MOMENTS, USER_MOMENTS, PLACE_MOMENTS
    }

    @Override
    @Transactional
    public MomentDetailResponse getMomentDetail(Long momentId, Long currentUserId) {
        Moment moment = momentRepository.findByIdWithImages(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        if (!moment.canBeViewedBy(currentUserId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        momentViewService.incrementViewCount(momentId);
        User author = userQueryService.getUserById(moment.getUserId());

        boolean isOwner = moment.isOwnedBy(currentUserId);
        Long commentCount = commentRepository.countByMomentIdAndNotDeleted(momentId);
        Long viewCount = momentViewService.getViewCount(momentId);

        return MomentDetailResponse.from(moment, isOwner, commentCount, viewCount, author);
    }

    private MomentListResponse buildMomentListResponse(List<Moment> moments, int pageSize, boolean includeAuthor, String currentCursor, String baseUrl) {
        boolean hasNext = moments.size() > pageSize;
        List<Moment> actualMoments = hasNext ? moments.subList(0, pageSize) : moments;

        if (actualMoments.isEmpty()) {
            return buildEmptyMomentListResponse(pageSize, baseUrl);
        }

        List<Long> momentIds = actualMoments.stream()
                .map(Moment::getId)
                .collect(Collectors.toList());

        Map<Long, Long> commentCountMap = getCommentCountMapWithCache(momentIds);
        Map<Long, Long> viewCountMap = getViewCountMapWithCache(actualMoments);

        List<MomentListResponse.MomentSummaryDto> momentDtos = actualMoments.stream()
                .map(moment -> buildMomentSummaryDto(moment, includeAuthor, commentCountMap, viewCountMap))
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasNext && !actualMoments.isEmpty()) {
            nextCursor = actualMoments.get(actualMoments.size() - 1)
                    .getCreatedAt()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        }

        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(pageSize)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();

        String selfHref = currentCursor != null
                ? String.format("%s?limit=%d&cursor=%s", baseUrl, pageSize, currentCursor)
                : String.format("%s?limit=%d", baseUrl, pageSize);

        String nextHref = hasNext && nextCursor != null
                ? String.format("%s?limit=%d&cursor=%s", baseUrl, pageSize, nextCursor)
                : null;

        MomentListResponse.LinksDto links = MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder().href(selfHref).build())
                .next(nextHref != null ? MomentListResponse.LinkDto.builder().href(nextHref).build() : null)
                .build();

        return MomentListResponse.builder()
                .moments(momentDtos)
                .meta(meta)
                .links(links)
                .build();
    }

    private MomentListResponse.MomentSummaryDto buildMomentSummaryDto(Moment moment, boolean includeAuthor,
                                                                      Map<Long, Long> commentCountMap,
                                                                      Map<Long, Long> viewCountMap) {
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
                .viewCount(viewCountMap.getOrDefault(moment.getId(), 0L));

        if (includeAuthor) {
            User author = userQueryService.getUserById(moment.getUserId());
            builder.author(MomentListResponse.AuthorDto.builder()
                    .id(author.getId())
                    .nickname(author.getUsername())
                    .profileImage(author.getImageUrl())
                    .build());
        }

        return builder.build();
    }

    private Map<Long, Long> getCommentCountMapWithCache(List<Long> momentIds) {
        if (momentIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, Long> cachedCommentCounts = momentCacheService.getCommentCounts(momentIds);
        List<Long> missedMomentIds = momentIds.stream()
                .filter(momentId -> !cachedCommentCounts.containsKey(momentId))
                .collect(Collectors.toList());

        Map<Long, Long> result = new HashMap<>(cachedCommentCounts);

        if (!missedMomentIds.isEmpty()) {
            Map<Long, Long> dbCommentCounts = getCommentCountMap(missedMomentIds);
            result.putAll(dbCommentCounts);
            momentCacheService.cacheCommentCountsBatch(dbCommentCounts);

            log.debug("댓글 수 조회: 캐시 히트 {}/{}, DB 조회 {}/{}",
                    cachedCommentCounts.size(), momentIds.size(),
                    missedMomentIds.size(), momentIds.size());
        }

        return result;
    }

    private Map<Long, Long> getCommentCountMap(List<Long> momentIds) {
        if (momentIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = commentRepository.countByMomentIds(momentIds);
        Map<Long, Long> commentCountMap = new HashMap<>();

        for (Object[] result : results) {
            Long momentId = (Long) result[0];
            Long count = (Long) result[1];
            commentCountMap.put(momentId, count);
        }

        for (Long momentId : momentIds) {
            commentCountMap.putIfAbsent(momentId, 0L);
        }

        return commentCountMap;
    }

    private int validateAndGetLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Map<Long, Long> getViewCountMapWithCache(List<Moment> moments) {
        List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());

        if (momentIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, Long> cachedViewCounts = momentCacheService.getViewCounts(momentIds);
        List<Long> missedMomentIds = momentIds.stream()
                .filter(momentId -> !cachedViewCounts.containsKey(momentId))
                .collect(Collectors.toList());

        Map<Long, Long> result = new HashMap<>(cachedViewCounts);

        if (!missedMomentIds.isEmpty()) {
            Map<Long, Long> dbViewCounts = new HashMap<>();
            for (Moment moment : moments) {
                if (missedMomentIds.contains(moment.getId())) {
                    dbViewCounts.put(moment.getId(), moment.getViewCount());
                }
            }
            result.putAll(dbViewCounts);
            momentCacheService.cacheViewCountsBatch(dbViewCounts);
        }

        return result;
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
                .self(MomentListResponse.LinkDto.builder().href(baseUrl + "?limit=" + pageSize).build())
                .next(null)
                .build();

        return MomentListResponse.builder()
                .moments(List.of())
                .meta(meta)
                .links(links)
                .build();
    }
}
