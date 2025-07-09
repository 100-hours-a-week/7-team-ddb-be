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
public class MomentMyQueryOperation extends MomentQueryTemplate {

    public MomentMyQueryOperation(MomentRepository momentRepository,
                                  UserQueryService userQueryService,
                                  CommentRepository commentRepository,
                                  MomentViewService momentViewService,
                                  MomentCacheService momentCacheService) {
        super(momentRepository, userQueryService, commentRepository, momentViewService, momentCacheService);
    }

    @Override
    protected List<Moment> fetchMoments(MomentQueryContext context, int queryLimit) {
        return momentRepository.findByUserIdWithVisibilityNative(
                context.getCurrentUserId(),
                true, // includePrivate = true (내 기록은 비공개도 포함)
                context.getCursor(),
                queryLimit
        );
    }

    @Override
    protected String generateBaseUrl(MomentQueryContext context) {
        return "/api/v1/users/me/moments";
    }

    @Override
    protected boolean shouldIncludeAuthor(MomentQueryContext context) {
        return false; // 내 기록에는 작성자 정보 불필요
    }
}
