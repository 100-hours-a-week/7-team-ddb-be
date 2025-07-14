package com.dolpin.domain.moment.service.template;

import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.MomentViewService;
import com.dolpin.domain.moment.service.cache.MomentCacheService;
import com.dolpin.domain.user.service.UserQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MomentUserQueryOperation extends MomentQueryTemplate {

    public MomentUserQueryOperation(MomentRepository momentRepository,
                                    UserQueryService userQueryService,
                                    CommentRepository commentRepository,
                                    MomentViewService momentViewService,
                                    MomentCacheService momentCacheService) {
        super(momentRepository, userQueryService, commentRepository, momentViewService, momentCacheService);
    }

    @Override
    protected void validateBeforeQuery(MomentQueryContext context) {
        // 대상 사용자가 존재하는지 확인
        userQueryService.getUserById(context.getTargetUserId());
    }

    @Override
    protected List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
        return momentRepository.findByUserIdWithVisibilityNative(
                context.getTargetUserId(),
                false, // includePrivate = false (다른 사용자의 비공개 기록은 제외)
                context.getCursor(),
                queryLimit
        );
    }

    @Override
    protected String generateBaseUrl(MomentQueryContext context) {
        return "/api/v1/users/" + context.getTargetUserId() + "/moments";
    }

    @Override
    protected boolean shouldIncludeAuthor(MomentQueryContext context) {
        return false; // 특정 사용자 기록에는 작성자 정보 불필요 (이미 알고 있음)
    }
}
