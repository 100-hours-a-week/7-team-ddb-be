package com.dolpin.domain.moment.service.query;

import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.MomentViewService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.constants.MomentTestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MomentQueryService 테스트")
class MomentQueryServiceTest {

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MomentViewService momentViewService;

    @InjectMocks
    private MomentQueryServiceImpl momentQueryService;

    private Moment testMoment;
    private Moment otherUserMoment;
    private Moment privateMoment;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testMoment = createTestMoment(MomentTestConstants.TEST_USER_ID, true);
        otherUserMoment = createTestMoment(MomentTestConstants.OTHER_USER_ID, true);
        privateMoment = createTestMoment(MomentTestConstants.TEST_USER_ID, false);
        testUser = createTestUser(MomentTestConstants.TEST_USER_ID);
        otherUser = createTestUser(MomentTestConstants.OTHER_USER_ID);
    }

    @Nested
    @DisplayName("전체 Moment 목록 조회 테스트")
    class GetAllMomentsTest {

        @Test
        @DisplayName("로그인 사용자 - 공개 기록과 본인 비공개 기록 조회")
        void getAllMoments_AuthenticatedUser() {
            // given
            List<Moment> moments = List.of(testMoment, otherUserMoment, privateMoment);
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(moments);

            setupCommentCountMocks(moments);
            setupUserMocks();

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getMoments()).hasSize(3);
            assertThat(response.getMeta().getPagination().getHasNext()).isFalse();
            assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // 작성자 정보가 포함되어야 함 (includeAuthor = true)
            assertThat(response.getMoments().get(0).getAuthor()).isNotNull();
            assertThat(response.getMoments().get(0).getAuthor().getId()).isEqualTo(MomentTestConstants.TEST_USER_ID);
        }

        @Test
        @DisplayName("익명 사용자 - 공개 기록만 조회")
        void getAllMoments_AnonymousUser() {
            // given
            List<Moment> publicMoments = List.of(testMoment, otherUserMoment);
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    null, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(publicMoments);

            setupCommentCountMocks(publicMoments);
            setupUserMocks();

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    null, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getMoments()).hasSize(2);
            assertThat(response.getMoments()).allMatch(moment -> moment.getIsPublic());
        }

        @Test
        @DisplayName("페이지네이션 - hasNext true")
        void getAllMoments_HasNext() {
            // given
            List<Moment> moments = List.of(testMoment, otherUserMoment, privateMoment); // limit + 1개
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, 3))
                    .willReturn(moments);

            setupCommentCountMocks(moments.subList(0, 2)); // 실제로는 2개만 반환
            setupUserMocks();

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, 2, null);

            // then
            assertThat(response.getMoments()).hasSize(2);
            assertThat(response.getMeta().getPagination().getHasNext()).isTrue();
            assertThat(response.getMeta().getPagination().getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("커서 기반 페이지네이션")
        void getAllMoments_WithCursor() {
            // given
            List<Moment> moments = List.of(otherUserMoment);
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_CURSOR, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(moments);

            List<Object[]> commentCounts = new ArrayList<>();
            commentCounts.add(new Object[] {
                    MomentTestConstants.TEST_MOMENT_ID,
                    MomentTestConstants.DEFAULT_COMMENT_COUNT
            });
            given(commentRepository.countByMomentIds(List.of(MomentTestConstants.TEST_MOMENT_ID)))
                    .willReturn(commentCounts);

            given(userQueryService.getUserById(MomentTestConstants.OTHER_USER_ID)).willReturn(otherUser);

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, MomentTestConstants.TEST_CURSOR);

            // then
            assertThat(response.getMoments()).hasSize(1);
            assertThat(response.getLinks().getSelf().getHref()).contains("cursor=" + MomentTestConstants.TEST_CURSOR);
        }
    }

    @Nested
    @DisplayName("내 Moment 목록 조회 테스트")
    class GetMyMomentsTest {

        @Test
        @DisplayName("내 기록 전체 조회 - 공개/비공개 모두 포함")
        void getMyMoments_AllMyMoments() {
            // given
            List<Moment> myMoments = List.of(testMoment, privateMoment);
            given(momentRepository.findByUserIdWithVisibilityNative(
                    MomentTestConstants.TEST_USER_ID, true, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(myMoments);

            setupCommentCountMocks(myMoments);

            // when
            MomentListResponse response = momentQueryService.getMyMoments(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getMoments()).hasSize(2);
            assertThat(response.getMoments()).extracting("author").containsOnlyNulls(); // includeAuthor = false

            // 내 기록이므로 공개/비공개 모두 포함
            assertThat(response.getMoments()).anyMatch(moment -> moment.getIsPublic());
            assertThat(response.getMoments()).anyMatch(moment -> !moment.getIsPublic());
        }

        @Test
        @DisplayName("빈 결과 처리")
        void getMyMoments_EmptyResult() {
            // given
            given(momentRepository.findByUserIdWithVisibilityNative(
                    MomentTestConstants.TEST_USER_ID, true, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(List.of());

            // when
            MomentListResponse response = momentQueryService.getMyMoments(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getMoments()).isEmpty();
            assertThat(response.getMeta().getPagination().getHasNext()).isFalse();
            assertThat(response.getMeta().getPagination().getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("다른 사용자 Moment 목록 조회 테스트")
    class GetUserMomentsTest {

        @Test
        @DisplayName("다른 사용자 공개 기록만 조회")
        void getUserMoments_PublicOnly() {
            // given
            given(userQueryService.getUserById(MomentTestConstants.OTHER_USER_ID))
                    .willReturn(otherUser);

            List<Moment> publicMoments = List.of(otherUserMoment);
            given(momentRepository.findByUserIdWithVisibilityNative(
                    MomentTestConstants.OTHER_USER_ID, false, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(publicMoments);

            setupCommentCountMocks(publicMoments);

            // when
            MomentListResponse response = momentQueryService.getUserMoments(
                    MomentTestConstants.OTHER_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getMoments()).hasSize(1);
            assertThat(response.getMoments()).allMatch(moment -> moment.getIsPublic());

            then(userQueryService).should(times(1)).getUserById(MomentTestConstants.OTHER_USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
        void getUserMoments_UserNotFound() {
            // given
            given(userQueryService.getUserById(MomentTestConstants.OTHER_USER_ID))
                    .willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> momentQueryService.getUserMoments(
                    MomentTestConstants.OTHER_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                    .isInstanceOf(BusinessException.class);

            then(momentRepository).should(times(0))
                    .findByUserIdWithVisibilityNative(any(Long.class), any(Boolean.class), any(String.class), any(Integer.class));
        }
    }

    @Nested
    @DisplayName("장소별 Moment 목록 조회 테스트")
    class GetPlaceMomentsTest {

        @Test
        @DisplayName("장소별 공개 기록 조회")
        void getPlaceMoments_PublicMomentsOnly() {
            // given
            List<Moment> placeMoments = List.of(testMoment, otherUserMoment);
            given(momentRepository.findPublicMomentsByPlaceIdNative(
                    MomentTestConstants.TEST_PLACE_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(placeMoments);

            setupCommentCountMocks(placeMoments);
            setupUserMocks();

            // when
            MomentListResponse response = momentQueryService.getPlaceMoments(
                    MomentTestConstants.TEST_PLACE_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getMoments()).hasSize(2);
            assertThat(response.getMoments()).allMatch(moment -> moment.getIsPublic());
            assertThat(response.getMoments().get(0).getAuthor()).isNotNull(); // includeAuthor = true
        }
    }

    @Nested
    @DisplayName("Moment 상세 조회 테스트")
    class GetMomentDetailTest {

        @Test
        @DisplayName("정상적인 상세 조회 - 소유자")
        void getMomentDetail_Success_Owner() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(userQueryService.getUserById(MomentTestConstants.TEST_USER_ID))
                    .willReturn(testUser);
            given(commentRepository.countByMomentIdAndNotDeleted(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(MomentTestConstants.DEFAULT_COMMENT_COUNT);
            given(momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(MomentTestConstants.UPDATED_VIEW_COUNT);

            willDoNothing().given(momentViewService).incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // when
            MomentDetailResponse response = momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.TEST_USER_ID);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            assertThat(response.getIsOwner()).isTrue();
            assertThat(response.getCommentCount()).isEqualTo(MomentTestConstants.DEFAULT_COMMENT_COUNT);
            assertThat(response.getViewCount()).isEqualTo(MomentTestConstants.UPDATED_VIEW_COUNT);
            assertThat(response.getAuthor().getId()).isEqualTo(MomentTestConstants.TEST_USER_ID);

            then(momentViewService).should(times(1)).incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("정상적인 상세 조회 - 다른 사용자")
        void getMomentDetail_Success_OtherUser() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(userQueryService.getUserById(MomentTestConstants.TEST_USER_ID))
                    .willReturn(testUser);
            given(commentRepository.countByMomentIdAndNotDeleted(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(MomentTestConstants.DEFAULT_COMMENT_COUNT);
            given(momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(MomentTestConstants.DEFAULT_VIEW_COUNT);

            willDoNothing().given(momentViewService).incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // when
            MomentDetailResponse response = momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.OTHER_USER_ID);

            // then
            assertThat(response.getIsOwner()).isFalse();
            then(momentViewService).should(times(1)).incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 상세 조회")
        void getMomentDetail_MomentNotFound() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.TEST_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.MOMENT_NOT_FOUND_MESSAGE);

            then(momentViewService).should(times(0)).incrementViewCount(any());
        }

        @Test
        @DisplayName("비공개 기록 접근 권한 없음")
        void getMomentDetail_AccessDenied() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(privateMoment));

            // when & then
            assertThatThrownBy(() -> momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.ACCESS_DENIED_MESSAGE);

            then(momentViewService).should(times(0)).incrementViewCount(any());
        }

        @Test
        @DisplayName("익명 사용자 비공개 기록 접근")
        void getMomentDetail_AnonymousUserPrivateMoment() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(privateMoment));

            // when & then
            assertThatThrownBy(() -> momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.ACCESS_DENIED_MESSAGE);
        }
    }

    @Nested
    @DisplayName("페이지네이션 검증 테스트")
    class PaginationValidationTest {

        @Test
        @DisplayName("limit이 null인 경우 기본값 사용")
        void validateAndGetLimit_NullLimit() {
            // given
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(List.of());

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, null, null);

            // then
            assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(MomentTestConstants.DEFAULT_PAGE_LIMIT);
        }

        @Test
        @DisplayName("limit이 0 이하인 경우 기본값 사용")
        void validateAndGetLimit_InvalidLimit() {
            // given
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(List.of());

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, -5, null);

            // then
            assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(MomentTestConstants.DEFAULT_PAGE_LIMIT);
        }

        @Test
        @DisplayName("limit이 최대값보다 큰 경우 최대값 사용")
        void validateAndGetLimit_ExceedsMaxLimit() {
            // given
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, MomentTestConstants.MAX_PAGE_LIMIT + 1))
                    .willReturn(List.of());

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, 100, null);

            // then
            assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(MomentTestConstants.MAX_PAGE_LIMIT);
        }
    }

    @Nested
    @DisplayName("링크 생성 검증 테스트")
    class LinksGenerationTest {

        @Test
        @DisplayName("self 링크 생성 - 커서 없음")
        void generateLinks_SelfWithoutCursor() {
            // given
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT + 1))
                    .willReturn(List.of(testMoment));
            setupCommentCountMocks(List.of(testMoment));
            // 작성자 정보가 필요한 경우에만 setupUserMocks 호출
            given(userQueryService.getUserById(MomentTestConstants.TEST_USER_ID)).willReturn(testUser);

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.DEFAULT_PAGE_LIMIT, null);

            // then
            assertThat(response.getLinks().getSelf().getHref())
                    .isEqualTo("/api/v1/users/moments?limit=" + MomentTestConstants.DEFAULT_PAGE_LIMIT);
            assertThat(response.getLinks().getNext()).isNull();
        }

        @Test
        @DisplayName("next 링크 생성 - hasNext true")
        void generateLinks_NextWithHasNext() {
            // given
            List<Moment> moments = List.of(testMoment, otherUserMoment);
            given(momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, 2))
                    .willReturn(moments);
            setupCommentCountMocks(List.of(testMoment)); // 실제로는 1개만 반환
            // 작성자 정보가 필요한 경우에만 필요한 사용자만 mock
            given(userQueryService.getUserById(MomentTestConstants.TEST_USER_ID)).willReturn(testUser);

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, 1, null);

            // then
            assertThat(response.getLinks().getNext()).isNotNull();
            assertThat(response.getLinks().getNext().getHref()).contains("cursor=");
        }
    }

    // Helper methods
    private Moment createTestMoment(Long userId, boolean isPublic) {
        return Moment.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .userId(userId)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .isPublic(isPublic)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User createTestUser(Long userId) {
        return User.builder()
                .id(userId)
                .username(MomentTestConstants.TEST_USERNAME)
                .imageUrl(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    private void setupCommentCountMocks(List<Moment> moments) {
        if (moments.isEmpty()) {
            given(commentRepository.countByMomentIds(List.of())).willReturn(List.of());
            return;
        }

        List<Long> momentIds = moments.stream().map(Moment::getId).toList();
        List<Object[]> commentCounts = momentIds.stream()
                .map(id -> new Object[]{id, MomentTestConstants.DEFAULT_COMMENT_COUNT})
                .toList();

        given(commentRepository.countByMomentIds(momentIds)).willReturn(commentCounts);
    }

    private void setupUserMocks() {
        given(userQueryService.getUserById(MomentTestConstants.TEST_USER_ID)).willReturn(testUser);
        given(userQueryService.getUserById(MomentTestConstants.OTHER_USER_ID)).willReturn(otherUser);
    }
}
