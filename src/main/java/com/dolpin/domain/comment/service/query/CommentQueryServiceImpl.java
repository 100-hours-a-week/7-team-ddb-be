package com.dolpin.domain.comment.service.query;

import com.dolpin.domain.comment.dto.response.CommentListResponse;
import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentQueryServiceImpl implements CommentQueryService {

    private final CommentRepository commentRepository;
    private final MomentRepository momentRepository;
    private final UserQueryService userQueryService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByMomentId(Long momentId, Integer limit, String cursor, Long currentUserId) {
        // 기록 존재 및 접근 권한 확인
        Moment moment = validateMomentAccess(momentId, currentUserId);

        int pageSize = validateAndGetLimit(limit);
        String cursorString = cursor; // String 그대로 사용
        int queryLimit = pageSize + 1; // hasNext 판단용

        List<Comment> comments;

        if (cursorString != null && !cursorString.trim().isEmpty()) {
            // 커서 기반 페이지네이션 (네이티브 쿼리)
            comments = commentRepository.findByMomentIdAndNotDeletedWithCursorNative(momentId, cursorString, queryLimit);
        } else {
            // 첫 페이지 조회 (기존 JPQL 쿼리 사용)
            Pageable pageable = PageRequest.of(0, queryLimit);
            comments = commentRepository.findByMomentIdAndNotDeleted(momentId, pageable).getContent();
        }

        return buildCommentListResponse(comments, pageSize, currentUserId, momentId);
    }

    private Moment validateMomentAccess(Long momentId, Long currentUserId) {
        Moment moment = momentRepository.findBasicMomentById(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        if (!moment.canBeViewedBy(currentUserId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        return moment;
    }

    private CommentListResponse buildCommentListResponse(List<Comment> comments, int pageSize, Long currentUserId, Long momentId) {
        boolean hasNext = comments.size() > pageSize;
        List<Comment> actualComments = hasNext ? comments.subList(0, pageSize) : comments;

        // 사용자 정보 일괄 조회
        List<Long> userIds = actualComments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userIds.stream()
                .map(userQueryService::getUserById)
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<CommentListResponse.CommentDto> commentDtos = actualComments.stream()
                .map(comment -> buildCommentDto(comment, userMap.get(comment.getUserId()), currentUserId))
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasNext && !actualComments.isEmpty()) {
            nextCursor = actualComments.get(actualComments.size() - 1)
                    .getCreatedAt()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        }

        CommentListResponse.MetaDto meta = CommentListResponse.MetaDto.builder()
                .pagination(CommentListResponse.PaginationDto.builder()
                        .limit(pageSize)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();

        String baseUrl = String.format("/api/v1/moments/%d/comments", momentId);
        CommentListResponse.LinksDto links = CommentListResponse.LinksDto.builder()
                .self(CommentListResponse.LinkDto.builder()
                        .href(String.format("%s?limit=%d", baseUrl, pageSize))
                        .build())
                .next(hasNext ? CommentListResponse.LinkDto.builder()
                        .href(String.format("%s?limit=%d&cursor=%s", baseUrl, pageSize, nextCursor))
                        .build() : null)
                .build();

        return CommentListResponse.builder()
                .comments(commentDtos)
                .meta(meta)
                .links(links)
                .build();
    }

    private CommentListResponse.CommentDto buildCommentDto(Comment comment, User user, Long currentUserId) {
        return CommentListResponse.CommentDto.builder()
                .id(comment.getId())
                .user(CommentListResponse.UserDto.builder()
                        .id(user.getId())
                        .nickname(user.getUsername())
                        .profileImage(user.getImageUrl())
                        .build())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .isOwner(comment.isOwnedBy(currentUserId))
                .build();
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
}
