package com.dolpin.domain.moment.service.query;

import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.MomentViewService;
import com.dolpin.domain.moment.service.cache.MomentCacheService;
import com.dolpin.domain.moment.service.template.*;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentQueryServiceImpl implements MomentQueryService {

    private final MomentRepository momentRepository;
    private final UserQueryService userQueryService;
    private final CommentRepository commentRepository;
    private final MomentViewService momentViewService;
    private final MomentCacheService momentCacheService;

    // Template Method 패턴 Operation들
    private final MomentAllQueryOperation momentAllQueryOperation;
    private final MomentMyQueryOperation momentMyQueryOperation;
    private final MomentUserQueryOperation momentUserQueryOperation;
    private final MomentPlaceQueryOperation momentPlaceQueryOperation;

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getAllMoments(Long currentUserId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.forAllMoments(currentUserId, limit, cursor);
        return momentAllQueryOperation.executeMomentQuery(context);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getMyMoments(Long userId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.forMyMoments(userId, limit, cursor);
        return momentMyQueryOperation.executeMomentQuery(context);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getUserMoments(Long targetUserId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.forUserMoments(targetUserId, limit, cursor);
        return momentUserQueryOperation.executeMomentQuery(context);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getPlaceMoments(Long placeId, Integer limit, String cursor) {
        MomentQueryContext context = MomentQueryContext.forPlaceMoments(placeId, limit, cursor);
        return momentPlaceQueryOperation.executeMomentQuery(context);
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
}
