package com.dolpin.domain.moment.service.template;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MomentQueryContext {
    private MomentQueryType queryType;
    private Long currentUserId;
    private Long targetUserId;
    private Long placeId;
    private Integer limit;
    private String cursor;

    // 편의 메서드들
    public static MomentQueryContext forAllMoments(Long currentUserId, Integer limit, String cursor) {
        return MomentQueryContext.builder()
                .queryType(MomentQueryType.ALL_MOMENTS)
                .currentUserId(currentUserId)
                .limit(limit)
                .cursor(cursor)
                .build();
    }

    public static MomentQueryContext forMyMoments(Long userId, Integer limit, String cursor) {
        return MomentQueryContext.builder()
                .queryType(MomentQueryType.MY_MOMENTS)
                .currentUserId(userId)
                .limit(limit)
                .cursor(cursor)
                .build();
    }

    public static MomentQueryContext forUserMoments(Long targetUserId, Integer limit, String cursor) {
        return MomentQueryContext.builder()
                .queryType(MomentQueryType.USER_MOMENTS)
                .targetUserId(targetUserId)
                .limit(limit)
                .cursor(cursor)
                .build();
    }

    public static MomentQueryContext forPlaceMoments(Long placeId, Integer limit, String cursor) {
        return MomentQueryContext.builder()
                .queryType(MomentQueryType.PLACE_MOMENTS)
                .placeId(placeId)
                .limit(limit)
                .cursor(cursor)
                .build();
    }
}
