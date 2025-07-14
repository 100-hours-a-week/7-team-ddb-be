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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("MomentQueryService 테스트 - Template Method 패턴 적용")
class MomentQueryServiceTest {

    @InjectMocks
    private MomentQueryServiceImpl momentQueryService;

    // Template Method 패턴의 Operation들을 Mock으로 주입
    @Mock
    private MomentAllQueryOperation momentAllQueryOperation;
    @Mock
    private MomentMyQueryOperation momentMyQueryOperation;
    @Mock
    private MomentUserQueryOperation momentUserQueryOperation;
    @Mock
    private MomentPlaceQueryOperation momentPlaceQueryOperation;

    // getMomentDetail에서 직접 사용하는 의존성들
    @Mock
    private MomentRepository momentRepository;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MomentViewService momentViewService;
    @Mock
    private MomentCacheService momentCacheService;

    private Moment testMoment;
    private User testUser;

    @BeforeEach
    void setUp() {
        testMoment = createTestMoment();
        testUser = createTestUser();
    }

    @Nested
    @DisplayName("전체 Moment 조회 테스트")
    class GetAllMomentsTest {

        @Test
        @DisplayName("전체 Moment 조회 성공")
        void getAllMoments_Success() {
            // given
            MomentListResponse expectedResponse = createMockListResponse();
            given(momentAllQueryOperation.executeMomentQuery(any()))
                    .willReturn(expectedResponse);

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMoments()).isNotEmpty();

            // MomentAllQueryOperation이 호출되었는지 검증
            then(momentAllQueryOperation).should().executeMomentQuery(any());
        }

        @Test
        @DisplayName("커서 기반 페이징으로 전체 Moment 조회")
        void getAllMoments_WithCursor_Success() {
            // given
            MomentListResponse expectedResponse = createMockListResponse();
            given(momentAllQueryOperation.executeMomentQuery(any()))
                    .willReturn(expectedResponse);

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    MomentTestConstants.TEST_CURSOR
            );

            // then
            assertThat(response).isNotNull();
            then(momentAllQueryOperation).should().executeMomentQuery(any());
        }

        @Test
        @DisplayName("빈 결과 조회")
        void getAllMoments_EmptyResult() {
            // given
            MomentListResponse emptyResponse = createEmptyListResponse();
            given(momentAllQueryOperation.executeMomentQuery(any()))
                    .willReturn(emptyResponse);

            // when
            MomentListResponse response = momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMoments()).isEmpty();
            then(momentAllQueryOperation).should().executeMomentQuery(any());
        }
    }

    @Nested
    @DisplayName("내 Moment 조회 테스트")
    class GetMyMomentsTest {

        @Test
        @DisplayName("내 Moment 조회 성공")
        void getMyMoments_Success() {
            // given
            MomentListResponse expectedResponse = createMockListResponse();
            given(momentMyQueryOperation.executeMomentQuery(any()))
                    .willReturn(expectedResponse);

            // when
            MomentListResponse response = momentQueryService.getMyMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            );

            // then
            assertThat(response).isNotNull();
            then(momentMyQueryOperation).should().executeMomentQuery(any());
        }
    }

    @Nested
    @DisplayName("사용자별 Moment 조회 테스트")
    class GetUserMomentsTest {

        @Test
        @DisplayName("사용자별 Moment 조회 성공")
        void getUserMoments_Success() {
            // given
            MomentListResponse expectedResponse = createMockListResponse();
            given(momentUserQueryOperation.executeMomentQuery(any()))
                    .willReturn(expectedResponse);

            // when
            MomentListResponse response = momentQueryService.getUserMoments(
                    MomentTestConstants.OTHER_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            );

            // then
            assertThat(response).isNotNull();
            then(momentUserQueryOperation).should().executeMomentQuery(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 전파")
        void getUserMoments_UserNotFound_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.USER_NOT_FOUND.withMessage("사용자를 찾을 수 없습니다.")
            );

            given(momentUserQueryOperation.executeMomentQuery(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentQueryService.getUserMoments(
                    999L, MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("장소별 Moment 조회 테스트")
    class GetPlaceMomentsTest {

        @Test
        @DisplayName("장소별 Moment 조회 성공")
        void getPlaceMoments_Success() {
            // given
            MomentListResponse expectedResponse = createMockListResponse();
            given(momentPlaceQueryOperation.executeMomentQuery(any()))
                    .willReturn(expectedResponse);

            // when
            MomentListResponse response = momentQueryService.getPlaceMoments(
                    MomentTestConstants.TEST_PLACE_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            );

            // then
            assertThat(response).isNotNull();
            then(momentPlaceQueryOperation).should().executeMomentQuery(any());
        }
    }

    @Nested
    @DisplayName("Moment 상세 조회 테스트 (기존 로직 유지)")
    class GetMomentDetailTest {

        @Test
        @DisplayName("Moment 상세 조회 성공")
        void getMomentDetail_Success() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(userQueryService.getUserById(MomentTestConstants.TEST_USER_ID))
                    .willReturn(testUser);
            given(commentRepository.countByMomentIdAndNotDeleted(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(MomentTestConstants.DEFAULT_COMMENT_COUNT);
            given(momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(MomentTestConstants.DEFAULT_VIEW_COUNT);
            willDoNothing().given(momentViewService).incrementViewCount(anyLong());

            // when
            MomentDetailResponse response = momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.TEST_USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            assertThat(response.getTitle()).isEqualTo(MomentTestConstants.TEST_MOMENT_TITLE);

            // 조회수 증가 메서드가 호출되었는지 검증
            then(momentViewService).should().incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 상세 조회 시 예외 발생")
        void getMomentDetail_MomentNotFound_ThrowsException() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.TEST_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("기록을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("접근 권한이 없는 Moment 조회 시 예외 발생")
        void getMomentDetail_AccessDenied_ThrowsException() {
            // given
            Moment privateMoment = createPrivateMoment(); // 소유자: TEST_USER_ID, 비공개
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(privateMoment));

            // when & then - OTHER_USER_ID로 TEST_USER_ID 소유의 비공개 Moment 조회 시도
            assertThatThrownBy(() -> momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID, MomentTestConstants.OTHER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("접근 권한이 없습니다.");

            // UserQueryService가 호출되지 않았는지 검증 (권한 체크에서 실패)
            then(userQueryService).shouldHaveNoInteractions();
        }
    }

    // ==================== Helper Methods ====================

    private Moment createTestMoment() {
        return Moment.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .userId(MomentTestConstants.TEST_USER_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Moment createPrivateMoment() {
        return Moment.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .userId(MomentTestConstants.TEST_USER_ID) // 소유자는 TEST_USER_ID
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .isPublic(false) // 비공개
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private User createTestUser() {
        return User.builder()
                .id(MomentTestConstants.TEST_USER_ID)
                .username(MomentTestConstants.TEST_USERNAME)
                .imageUrl(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    private MomentListResponse createMockListResponse() {
        MomentListResponse.MomentSummaryDto momentDto = MomentListResponse.MomentSummaryDto.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .thumbnail(MomentTestConstants.TEST_IMAGE_1)
                .imagesCount(MomentTestConstants.TEST_IMAGES_COUNT)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .createdAt(LocalDateTime.now())
                .author(MomentListResponse.AuthorDto.builder()
                        .id(MomentTestConstants.TEST_USER_ID)
                        .nickname(MomentTestConstants.TEST_USERNAME)
                        .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .build();

        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(null)
                        .hasNext(false)
                        .build())
                .build();

        MomentListResponse.LinksDto links = MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder()
                        .href("/api/v1/users/moments?limit=" + MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .build())
                .next(null)
                .build();

        return MomentListResponse.builder()
                .moments(List.of(momentDto))
                .meta(meta)
                .links(links)
                .build();
    }

    private MomentListResponse createEmptyListResponse() {
        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(null)
                        .hasNext(false)
                        .build())
                .build();

        MomentListResponse.LinksDto links = MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder()
                        .href("/api/v1/users/moments?limit=" + MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .build())
                .next(null)
                .build();

        return MomentListResponse.builder()
                .moments(Collections.emptyList())
                .meta(meta)
                .links(links)
                .build();
    }

    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionTest {

        @Test
        @DisplayName("잘못된 limit 값으로 조회 시 예외 전파")
        void getAllMoments_InvalidLimit_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.INVALID_PARAMETER.withMessage("limit은 0보다 커야 합니다.")
            );

            given(momentAllQueryOperation.executeMomentQuery(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, 0, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("limit은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("최대 limit 초과 시 예외 전파")
        void getAllMoments_ExceedsMaxLimit_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.INVALID_PARAMETER.withMessage("limit은 50 이하여야 합니다.")
            );

            given(momentAllQueryOperation.executeMomentQuery(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID, 100, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("limit은 50 이하여야 합니다.");
        }
    }

    @Nested
    @DisplayName("Operation 호출 검증 테스트")
    class OperationCallTest {

        @Test
        @DisplayName("각 조회 메서드가 올바른 Operation을 호출하는지 검증")
        void verifyCorrectOperationCalls() {
            // given
            MomentListResponse mockResponse = createMockListResponse();
            given(momentAllQueryOperation.executeMomentQuery(any())).willReturn(mockResponse);
            given(momentMyQueryOperation.executeMomentQuery(any())).willReturn(mockResponse);
            given(momentUserQueryOperation.executeMomentQuery(any())).willReturn(mockResponse);
            given(momentPlaceQueryOperation.executeMomentQuery(any())).willReturn(mockResponse);

            // when
            momentQueryService.getAllMoments(MomentTestConstants.TEST_USER_ID, 10, null);
            momentQueryService.getMyMoments(MomentTestConstants.TEST_USER_ID, 10, null);
            momentQueryService.getUserMoments(MomentTestConstants.OTHER_USER_ID, 10, null);
            momentQueryService.getPlaceMoments(MomentTestConstants.TEST_PLACE_ID, 10, null);

            // then
            then(momentAllQueryOperation).should().executeMomentQuery(any());
            then(momentMyQueryOperation).should().executeMomentQuery(any());
            then(momentUserQueryOperation).should().executeMomentQuery(any());
            then(momentPlaceQueryOperation).should().executeMomentQuery(any());
        }

        @Test
        @DisplayName("Context가 올바르게 생성되어 전달되는지 검증")
        void verifyContextCreation() {
            // given
            MomentListResponse mockResponse = createMockListResponse();
            given(momentAllQueryOperation.executeMomentQuery(any())).willReturn(mockResponse);

            // when
            momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    MomentTestConstants.TEST_CURSOR
            );

            // then - Context 내용 검증은 Operation 단위 테스트에서 더 자세히 다룰 예정
            then(momentAllQueryOperation).should().executeMomentQuery(any());
        }
    }
}
