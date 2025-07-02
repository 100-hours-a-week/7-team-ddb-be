package com.dolpin.domain.moment.service.query;

import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.dto.response.PlaceMomentListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getAllMoments(Long currentUserId, Integer limit, String cursor) {
        int pageSize = validateAndGetLimit(limit);
        String cursorString = cursor;
        int queryLimit = pageSize + 1;

        List<Moment> moments = momentRepository.findPublicMomentsWithUserPrivateNative(currentUserId, cursorString, queryLimit);

        return buildMomentListResponse(moments, pageSize, true, cursor, "/api/v1/users/moments");
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getMyMoments(Long userId, Integer limit, String cursor) {
        int pageSize = validateAndGetLimit(limit);
        String cursorString = cursor;
        int queryLimit = pageSize + 1;

        List<Moment> moments = momentRepository.findByUserIdWithVisibilityNative(userId, true, cursorString, queryLimit);

        return buildMomentListResponse(moments, pageSize, false, cursor, "/api/v1/users/me/moments");
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getUserMoments(Long targetUserId, Integer limit, String cursor) {
        userQueryService.getUserById(targetUserId);

        int pageSize = validateAndGetLimit(limit);
        String cursorString = cursor;
        int queryLimit = pageSize + 1;

        List<Moment> moments = momentRepository.findByUserIdWithVisibilityNative(targetUserId, false, cursorString, queryLimit);

        return buildMomentListResponse(moments, pageSize, false, cursor, "/api/v1/users/" + targetUserId + "/moments");
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getPlaceMoments(Long placeId, Integer limit, String cursor) {
        int pageSize = validateAndGetLimit(limit);
        String cursorString = cursor;
        int queryLimit = pageSize + 1;

        List<Moment> moments = momentRepository.findPublicMomentsByPlaceIdNative(placeId, cursorString, queryLimit);

        return buildMomentListResponse(moments, pageSize, true, cursor, "/api/v1/places/" + placeId + "/moments");
    }


    @Override
    @Transactional
    public MomentDetailResponse getMomentDetail(Long momentId, Long currentUserId) {
        Moment moment = momentRepository.findByIdWithImages(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        if (!moment.canBeViewedBy(currentUserId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        // 조회수 증가
        momentViewService.incrementViewCount(momentId);

        User author = userQueryService.getUserById(moment.getUserId());

        boolean isOwner = moment.isOwnedBy(currentUserId);
        Long commentCount = commentRepository.countByMomentIdAndNotDeleted(momentId);
        Long viewCount = momentViewService.getViewCount(momentId);

        return MomentDetailResponse.from(moment, isOwner, commentCount, viewCount, author);
    }

    private PlaceMomentListResponse.PlaceMomentDto buildPlaceMomentDto(Moment moment) {
        String thumbnail = moment.getThumbnailUrl();
        User author = userQueryService.getUserById(moment.getUserId());

        return PlaceMomentListResponse.PlaceMomentDto.builder()
                .id(moment.getId())
                .title(moment.getTitle())
                .thumbnail(thumbnail)
                .imagesCount(moment.getImageCount())
                .isPublic(moment.getIsPublic())
                .createdAt(moment.getCreatedAt())
                .place(PlaceMomentListResponse.PlaceDto.builder()
                        .id(moment.getPlaceId())
                        .name(moment.getPlaceName())
                        .build())
                .author(PlaceMomentListResponse.AuthorDto.builder()
                        .id(author.getId())
                        .nickname(author.getUsername())
                        .profileImage(author.getImageUrl())
                        .build())
                .build();
    }

    private MomentListResponse buildMomentListResponse(List<Moment> moments, int pageSize, boolean includeAuthor, String currentCursor, String baseUrl) {
        boolean hasNext = moments.size() > pageSize;
        List<Moment> actualMoments = hasNext ? moments.subList(0, pageSize) : moments;

        // 빈 결과 처리 추가
        if (actualMoments.isEmpty()) {
            return buildEmptyMomentListResponse(pageSize, baseUrl);
        }

        // 댓글 수 한 번에 조회
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

        // _links href 값을 API 명세에 맞게 생성
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
                                                                      Map<Long, Long> viewCountMap) { // 👈 viewCountMap 파라미터 추가
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
                .viewCount(viewCountMap.getOrDefault(moment.getId(), 0L)); // 👈 캐시에서 조회한 값 사용

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

        // 1. 캐시에서 댓글 수 조회
        Map<Long, Long> cachedCommentCounts = momentCacheService.getCommentCounts(momentIds);

        // 2. 캐시 미스인 기록들 추출
        List<Long> missedMomentIds = momentIds.stream()
                .filter(momentId -> !cachedCommentCounts.containsKey(momentId))
                .collect(Collectors.toList());

        Map<Long, Long> result = new HashMap<>(cachedCommentCounts);

        // 3. 캐시 미스인 경우 DB에서 조회
        if (!missedMomentIds.isEmpty()) {
            Map<Long, Long> dbCommentCounts = getCommentCountMap(missedMomentIds); // 기존 메서드 활용
            result.putAll(dbCommentCounts);

            // 4. DB에서 조회한 데이터 캐싱
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

    /**
     * 캐시를 활용한 조회 수 조회
     */
    private Map<Long, Long> getViewCountMapWithCache(List<Moment> moments) {
        List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());

        if (momentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 캐시에서 조회 수 조회
        Map<Long, Long> cachedViewCounts = momentCacheService.getViewCounts(momentIds);

        // 캐시 미스인 기록들 추출
        List<Long> missedMomentIds = momentIds.stream()
                .filter(momentId -> !cachedViewCounts.containsKey(momentId))
                .collect(Collectors.toList());

        Map<Long, Long> result = new HashMap<>(cachedViewCounts);

        // 캐시 미스인 경우 Moment 엔티티에서 조회
        if (!missedMomentIds.isEmpty()) {
            Map<Long, Long> dbViewCounts = new HashMap<>();
            for (Moment moment : moments) {
                if (missedMomentIds.contains(moment.getId())) {
                    dbViewCounts.put(moment.getId(), moment.getViewCount());
                }
            }
            result.putAll(dbViewCounts);

            // DB에서 조회한 데이터 캐싱
            momentCacheService.cacheViewCountsBatch(dbViewCounts);
        }

        return result;
    }

    /**
     * 빈 응답 생성
     */
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
