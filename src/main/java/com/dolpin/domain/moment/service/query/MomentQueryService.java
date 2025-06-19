package com.dolpin.domain.moment.service.query;

import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.dto.response.PlaceMomentListResponse;

public interface MomentQueryService {

    MomentListResponse getAllMoments(Long currentUserId, Integer limit, String cursor);

    MomentListResponse getMyMoments(Long userId, Integer limit, String cursor);

    MomentListResponse getUserMoments(Long targetUserId, Integer limit, String cursor);

    MomentListResponse getPlaceMoments(Long placeId, Integer limit, String cursor);

    MomentDetailResponse getMomentDetail(Long momentId, Long currentUserId);
}
