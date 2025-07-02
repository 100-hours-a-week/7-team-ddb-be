package com.dolpin.domain.moment.service.command;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.global.constants.MomentTestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MomentCommandService 테스트")
class MomentCommandServiceTest {

    @Mock
    private MomentRepository momentRepository;

    @InjectMocks
    private MomentCommandServiceImpl momentCommandService;

    private Moment testMoment;
    private Moment savedMoment;
    private MomentCreateRequest createRequest;
    private MomentUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testMoment = createTestMoment();
        savedMoment = createSavedMoment();
        createRequest = createMomentCreateRequest();
        updateRequest = createMomentUpdateRequest();
    }

    @Nested
    @DisplayName("Moment 생성 테스트")
    class CreateMomentTest {

        @Test
        @DisplayName("정상적인 Moment 생성 - 이미지 없음")
        void createMoment_Success_NoImages() {
            // given
            MomentCreateRequest requestWithoutImages = MomentCreateRequest.builder()
                    .title(MomentTestConstants.TEST_MOMENT_TITLE)
                    .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.TEST_PLACE_NAME)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            MomentCreateResponse response = momentCommandService.createMoment(
                    MomentTestConstants.TEST_USER_ID, requestWithoutImages);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            assertThat(response.getCreatedAt()).isNotNull();

            ArgumentCaptor<Moment> momentCaptor = ArgumentCaptor.forClass(Moment.class);
            then(momentRepository).should(times(1)).save(momentCaptor.capture());

            Moment capturedMoment = momentCaptor.getValue();
            assertThat(capturedMoment.getUserId()).isEqualTo(MomentTestConstants.TEST_USER_ID);
            assertThat(capturedMoment.getTitle()).isEqualTo(MomentTestConstants.TEST_MOMENT_TITLE);
            assertThat(capturedMoment.getContent()).isEqualTo(MomentTestConstants.TEST_MOMENT_CONTENT);
            assertThat(capturedMoment.getPlaceId()).isEqualTo(MomentTestConstants.TEST_PLACE_ID);
            assertThat(capturedMoment.getPlaceName()).isEqualTo(MomentTestConstants.TEST_PLACE_NAME);
            assertThat(capturedMoment.getIsPublic()).isEqualTo(MomentTestConstants.DEFAULT_IS_PUBLIC);
        }

        @Test
        @DisplayName("정상적인 Moment 생성 - 이미지 포함")
        void createMoment_Success_WithImages() {
            // given
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            MomentCreateResponse response = momentCommandService.createMoment(
                    MomentTestConstants.TEST_USER_ID, createRequest);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);

            ArgumentCaptor<Moment> momentCaptor = ArgumentCaptor.forClass(Moment.class);
            then(momentRepository).should(times(1)).save(momentCaptor.capture());

            Moment capturedMoment = momentCaptor.getValue();
            assertThat(capturedMoment.getImageCount()).isEqualTo(MomentTestConstants.TEST_IMAGES_COUNT);
        }

        @Test
        @DisplayName("isPublic이 null인 경우 기본값 true 설정")
        void createMoment_DefaultIsPublicTrue() {
            // given
            MomentCreateRequest requestWithNullIsPublic = MomentCreateRequest.builder()
                    .title(MomentTestConstants.TEST_MOMENT_TITLE)
                    .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                    .isPublic(null)
                    .build();

            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.createMoment(MomentTestConstants.TEST_USER_ID, requestWithNullIsPublic);

            // then
            ArgumentCaptor<Moment> momentCaptor = ArgumentCaptor.forClass(Moment.class);
            then(momentRepository).should(times(1)).save(momentCaptor.capture());

            Moment capturedMoment = momentCaptor.getValue();
            assertThat(capturedMoment.getIsPublic()).isTrue();
        }

        @Test
        @DisplayName("빈 이미지 리스트인 경우 처리")
        void createMoment_EmptyImageList() {
            // given
            MomentCreateRequest requestWithEmptyImages = MomentCreateRequest.builder()
                    .title(MomentTestConstants.TEST_MOMENT_TITLE)
                    .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                    .images(List.of())
                    .build();

            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.createMoment(MomentTestConstants.TEST_USER_ID, requestWithEmptyImages);

            // then
            ArgumentCaptor<Moment> momentCaptor = ArgumentCaptor.forClass(Moment.class);
            then(momentRepository).should(times(1)).save(momentCaptor.capture());

            Moment capturedMoment = momentCaptor.getValue();
            assertThat(capturedMoment.getImageCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Moment 수정 테스트")
    class UpdateMomentTest {

        @Test
        @DisplayName("정상적인 Moment 수정 - 모든 필드")
        void updateMoment_Success_AllFields() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            MomentUpdateResponse response = momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest);

            // then
            assertThat(response.getId()).isEqualTo(MomentTestConstants.TEST_MOMENT_ID);
            assertThat(response.getUpdatedAt()).isNotNull();

            then(momentRepository).should(times(1))
                    .findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1)).save(testMoment);
        }

        @Test
        @DisplayName("부분 필드만 수정")
        void updateMoment_PartialUpdate() {
            // given
            MomentUpdateRequest partialUpdateRequest = new MomentUpdateRequest(
                    MomentTestConstants.UPDATED_MOMENT_TITLE,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, partialUpdateRequest);

            // then
            then(momentRepository).should(times(1))
                    .findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1)).save(testMoment);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 수정 시 예외 발생")
        void updateMoment_MomentNotFound() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.MOMENT_NOT_FOUND_MESSAGE);

            then(momentRepository).should(times(0)).save(any(Moment.class));
        }

        @Test
        @DisplayName("다른 사용자의 Moment 수정 시 예외 발생")
        void updateMoment_AccessDenied() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when & then
            assertThatThrownBy(() -> momentCommandService.updateMoment(
                    MomentTestConstants.OTHER_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.ACCESS_DENIED_MESSAGE);

            then(momentRepository).should(times(0)).save(any(Moment.class));
        }

        @Test
        @DisplayName("이미지만 교체")
        void updateMoment_OnlyImages() {
            // given
            MomentUpdateRequest imageOnlyRequest = new MomentUpdateRequest(
                    null,
                    null,
                    null,
                    null,
                    MomentTestConstants.UPDATED_IMAGES,
                    null
            );

            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, imageOnlyRequest);

            // then
            then(momentRepository).should(times(1))
                    .findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1)).save(testMoment);
        }
    }

    @Nested
    @DisplayName("Moment 삭제 테스트")
    class DeleteMomentTest {

        @Test
        @DisplayName("정상적인 Moment 삭제")
        void deleteMoment_Success() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when
            momentCommandService.deleteMoment(MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID);

            // then
            then(momentRepository).should(times(1))
                    .findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1)).delete(testMoment);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 삭제 시 예외 발생")
        void deleteMoment_MomentNotFound() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.USER_NOT_FOUND_MESSAGE);

            then(momentRepository).should(times(0)).delete(any(Moment.class));
        }

        @Test
        @DisplayName("다른 사용자의 Moment 삭제 시 예외 발생")
        void deleteMoment_AccessDenied() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    MomentTestConstants.OTHER_USER_ID, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.ACCESS_DENIED_MESSAGE);

            then(momentRepository).should(times(0)).delete(any(Moment.class));
        }
    }

    @Nested
    @DisplayName("권한 검증 테스트")
    class OwnershipValidationTest {

        @Test
        @DisplayName("소유자 확인 성공")
        void validateOwnership_Success() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when & then - 예외가 발생하지 않아야 함
            momentCommandService.deleteMoment(MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID);

            then(momentRepository).should(times(1)).delete(testMoment);
        }

        @Test
        @DisplayName("null userId로 권한 검증")
        void validateOwnership_NullUserId() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    null, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.ACCESS_DENIED_MESSAGE);
        }

        @Test
        @DisplayName("음수 userId로 권한 검증")
        void validateOwnership_NegativeUserId() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    -1L, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.ACCESS_DENIED_MESSAGE);
        }
    }

    @Nested
    @DisplayName("도메인 로직 검증 테스트")
    class DomainLogicTest {

        @Test
        @DisplayName("이미지 추가 도메인 메서드 호출 확인")
        void createMoment_ImageDomainMethodCalled() {
            // given
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.createMoment(MomentTestConstants.TEST_USER_ID, createRequest);

            // then
            ArgumentCaptor<Moment> momentCaptor = ArgumentCaptor.forClass(Moment.class);
            then(momentRepository).should(times(1)).save(momentCaptor.capture());

            Moment capturedMoment = momentCaptor.getValue();
            // 도메인 메서드가 호출되어 이미지가 추가되었는지 확인
            assertThat(capturedMoment.hasImages()).isTrue();
            assertThat(capturedMoment.getImageCount()).isEqualTo(MomentTestConstants.TEST_IMAGES_COUNT);
        }

        @Test
        @DisplayName("장소 정보 업데이트 도메인 메서드 호출 확인")
        void updateMoment_PlaceInfoUpdateCalled() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest);

            // then
            then(momentRepository).should(times(1)).save(testMoment);
            // 실제 도메인 메서드 호출로 인한 상태 변화는 통합 테스트에서 검증
        }

        @Test
        @DisplayName("이미지 교체 도메인 메서드 호출 확인")
        void updateMoment_ImageReplacementCalled() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));
            given(momentRepository.save(any(Moment.class))).willReturn(savedMoment);

            // when
            momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest);

            // then
            then(momentRepository).should(times(1)).save(testMoment);
        }
    }

    @Nested
    @DisplayName("예외 상황 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("Repository 저장 실패")
        void createMoment_SaveFailed() {
            // given
            given(momentRepository.save(any(Moment.class)))
                    .willThrow(new RuntimeException("Database save failed"));

            // when & then
            assertThatThrownBy(() -> momentCommandService.createMoment(
                    MomentTestConstants.TEST_USER_ID, createRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database save failed");
        }

        @Test
        @DisplayName("Repository 조회 실패")
        void updateMoment_FindFailed() {
            // given
            given(momentRepository.findByIdWithImages(MomentTestConstants.TEST_MOMENT_ID))
                    .willThrow(new RuntimeException("Database find failed"));

            // when & then
            assertThatThrownBy(() -> momentCommandService.updateMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID, updateRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database find failed");

            then(momentRepository).should(times(0)).save(any(Moment.class));
        }

        @Test
        @DisplayName("Repository 삭제 실패")
        void deleteMoment_DeleteFailed() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // willThrow를 사용하여 void 메서드 예외 설정
            willThrow(new RuntimeException("Database delete failed"))
                    .given(momentRepository).delete(testMoment);

            // when & then
            assertThatThrownBy(() -> momentCommandService.deleteMoment(
                    MomentTestConstants.TEST_USER_ID, MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database delete failed");
        }
    }

    // Helper methods
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
                .build();
    }

    private Moment createSavedMoment() {
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
                .updatedAt(LocalDateTime.now())
                .build();
    }

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
}
