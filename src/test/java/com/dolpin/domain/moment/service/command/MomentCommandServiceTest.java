package com.dolpin.domain.moment.service.command;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
import com.dolpin.domain.moment.service.template.MomentCreateOperation;
import com.dolpin.domain.moment.service.template.MomentDeleteOperation;
import com.dolpin.domain.moment.service.template.MomentUpdateOperation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("MomentCommandService 테스트 - Template Method 패턴 적용")
class MomentCommandServiceTest {

    @InjectMocks
    private MomentCommandServiceImpl momentCommandService;

    // Template Method 패턴의 Operation들을 Mock으로 주입
    @Mock
    private MomentCreateOperation momentCreateOperation;
    @Mock
    private MomentUpdateOperation momentUpdateOperation;
    @Mock
    private MomentDeleteOperation momentDeleteOperation;

    private MomentCreateRequest createRequest;
    private MomentUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        createRequest = createMomentCreateRequest();
        updateRequest = createMomentUpdateRequest();
    }

    @Nested
    @DisplayName("Moment 생성 테스트")
    class CreateMomentTest {

        @Test
        @DisplayName("정상적인 Moment 생성")
        void createMoment_Success() {
            // given
            MomentCreateResponse expectedResponse = createExpectedCreateResponse();
            given(momentCreateOperation.executeMomentOperation(any()))
                    .willReturn(expectedResponse);

            // when
            MomentCreateResponse response = momentCommandService.createMoment(
                    MomentTestConstants.TEST_USER_ID, createRequest);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            assertThat(response.getCreatedAt()).isNotNull();

            // MomentCreateOperation이 호출되었는지 검증
            then(momentCreateOperation).should().executeMomentOperation(any());
        }

        @Test
        @DisplayName("이미지 없는 Moment 생성")
        void createMoment_Success_NoImages() {
            // given
            MomentCreateRequest requestWithoutImages = MomentCreateRequest.builder()
                    .title(MomentTestConstants.TEST_MOMENT_TITLE)
                    .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.TEST_PLACE_NAME)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            MomentCreateResponse expectedResponse = createExpectedCreateResponse();
            given(momentCreateOperation.executeMomentOperation(any()))
                    .willReturn(expectedResponse);

            // when
            MomentCreateResponse response = momentCommandService.createMoment(
                    MomentTestConstants.TEST_USER_ID, requestWithoutImages);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            then(momentCreateOperation).should().executeMomentOperation(any());
        }

        @Test
        @DisplayName("Moment 생성 시 Operation에서 예외 발생")
        void createMoment_OperationThrowsException_PropagatesException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.INVALID_PARAMETER.withMessage("제목이 필요합니다.")
            );

            given(momentCreateOperation.executeMomentOperation(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentCommandService.createMoment(
                    MomentTestConstants.TEST_USER_ID, createRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("제목이 필요합니다.");
        }

        @Test
        @DisplayName("null userId로 생성 시 예외 전파")
        void createMoment_NullUserId_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.INVALID_PARAMETER.withMessage("사용자 ID가 필요합니다.")
            );

            given(momentCreateOperation.executeMomentOperation(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentCommandService.createMoment(null, createRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("사용자 ID가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("Moment 수정 테스트")
    class UpdateMomentTest {

        @Test
        @DisplayName("정상적인 Moment 수정")
        void updateMoment_Success() {
            // given
            MomentUpdateResponse expectedResponse = createExpectedUpdateResponse();
            given(momentUpdateOperation.executeMomentOperation(any()))
                    .willReturn(expectedResponse);

            // when
            MomentUpdateResponse response = momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            assertThat(response.getUpdatedAt()).isNotNull();

            // MomentUpdateOperation이 호출되었는지 검증
            then(momentUpdateOperation).should().executeMomentOperation(any());
        }

        @Test
        @DisplayName("부분 필드만 수정")
        void updateMoment_PartialUpdate() {
            // given
            MomentUpdateRequest partialUpdateRequest = new MomentUpdateRequest(
                    MomentTestConstants.UPDATED_MOMENT_TITLE,
                    null, null, null, null, null
            );

            MomentUpdateResponse expectedResponse = createExpectedUpdateResponse();
            given(momentUpdateOperation.executeMomentOperation(any()))
                    .willReturn(expectedResponse);

            // when
            MomentUpdateResponse response = momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, partialUpdateRequest);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            then(momentUpdateOperation).should().executeMomentOperation(any());
        }

        @Test
        @DisplayName("존재하지 않는 Moment 수정 시 예외 전파")
        void updateMoment_MomentNotFound_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")
            );

            given(momentUpdateOperation.executeMomentOperation(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("기록을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("다른 사용자의 Moment 수정 시 예외 전파")
        void updateMoment_AccessDenied_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다.")
            );

            given(momentUpdateOperation.executeMomentOperation(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentCommandService.updateMoment(
                    MomentTestConstants.OTHER_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("접근 권한이 없습니다.");
        }

        @Test
        @DisplayName("수정할 내용이 없는 경우 예외 전파")
        void updateMoment_NoUpdateContent_ThrowsException() {
            // given
            MomentUpdateRequest emptyUpdateRequest = new MomentUpdateRequest(
                    null, null, null, null, null, null
            );

            BusinessException expectedException = new BusinessException(
                    ResponseStatus.INVALID_PARAMETER.withMessage("수정할 내용이 없습니다.")
            );

            given(momentUpdateOperation.executeMomentOperation(any()))
                    .willThrow(expectedException);

            // when & then
            assertThatThrownBy(() -> momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, emptyUpdateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("수정할 내용이 없습니다.");
        }
    }

    @Nested
    @DisplayName("Moment 삭제 테스트")
    class DeleteMomentTest {

        @Test
        @DisplayName("정상적인 Moment 삭제")
        void deleteMoment_Success() {
            // given
            // void 메서드이므로 아무것도 반환하지 않음

            // when
            momentCommandService.deleteMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID);

            // then
            // MomentDeleteOperation이 호출되었는지 검증
            then(momentDeleteOperation).should().executeMomentOperation(any());
        }

        @Test
        @DisplayName("존재하지 않는 Moment 삭제 시 예외 전파")
        void deleteMoment_MomentNotFound_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")
            );

            willThrow(expectedException)
                    .given(momentDeleteOperation).executeMomentOperation(any());

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("기록을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("다른 사용자의 Moment 삭제 시 예외 전파")
        void deleteMoment_AccessDenied_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다.")
            );

            willThrow(expectedException)
                    .given(momentDeleteOperation).executeMomentOperation(any());

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    MomentTestConstants.OTHER_USER_ID, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("접근 권한이 없습니다.");
        }

        @Test
        @DisplayName("null 값 파라미터로 삭제 시 예외 전파")
        void deleteMoment_NullParameters_ThrowsException() {
            // given
            BusinessException expectedException = new BusinessException(
                    ResponseStatus.INVALID_PARAMETER.withMessage("기록 ID가 필요합니다.")
            );

            willThrow(expectedException)
                    .given(momentDeleteOperation).executeMomentOperation(any());

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    MomentTestConstants.TEST_USER_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("기록 ID가 필요합니다.");
        }
    }

    // ==================== Helper Methods ====================

    private MomentCreateRequest createMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    private MomentUpdateRequest createMomentUpdateRequest() {
        return new MomentUpdateRequest(
                MomentTestConstants.UPDATED_MOMENT_TITLE,
                MomentTestConstants.UPDATED_MOMENT_CONTENT,
                MomentTestConstants.UPDATED_PLACE_ID,
                MomentTestConstants.UPDATED_PLACE_NAME,
                MomentTestConstants.UPDATED_IMAGES,
                MomentTestConstants.UPDATED_IS_PUBLIC
        );
    }

    private MomentCreateResponse createExpectedCreateResponse() {
        return MomentCreateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private MomentUpdateResponse createExpectedUpdateResponse() {
        return MomentUpdateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
