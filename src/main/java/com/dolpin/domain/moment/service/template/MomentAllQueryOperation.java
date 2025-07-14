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
public class MomentAllQueryOperation extends MomentQueryTemplate {

    public MomentAllQueryOperation(MomentRepository momentRepository,
                                   UserQueryService userQueryService,
                                   CommentRepository commentRepository,
                                   MomentViewService momentViewService,
                                   MomentCacheService momentCacheService) {
        super(momentRepository, userQueryService, commentRepository, momentViewService, momentCacheService);
    }

    @Override
    protected List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
        return momentRepository.findPublicMomentsWithUserPrivateNative(
                context.getCurrentUserId(),
                context.getCursor(),
                queryLimit
        );
    }

    @Override
    protected String generateBaseUrl(MomentQueryContext context) {
        return "/api/v1/users/moments";
    }

    @Override
    protected boolean shouldIncludeAuthor(MomentQueryContext context) {
        return true; // 전체 기록 조회 시 작성자 정보 포함
    }
}
