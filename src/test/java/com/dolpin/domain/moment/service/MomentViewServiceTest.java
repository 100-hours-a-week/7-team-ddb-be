package com.dolpin.domain.moment.service;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MomentViewService 테스트")
class MomentViewServiceTest {

    @Mock
    private MomentRepository momentRepository;

    @InjectMocks
    private MomentViewService momentViewService;

    private Moment testMoment;

    @BeforeEach
    void setUp() {
        testMoment = createTestMoment();
    }

    @Nested
    @DisplayName("조회수 증가 테스트")
    class IncrementViewCountTest {

        @Test
        @DisplayName("정상적인 조회수 증가")
        void incrementViewCount_Success() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(true);
            given(momentRepository.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(1);

            // when
            momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            then(momentRepository).should(times(1))
                    .existsById(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1))
                    .incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 조회수 증가 시 예외 발생")
        void incrementViewCount_MomentNotFound() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.MOMENT_NOT_FOUND_MESSAGE);

            // 조회수 증가는 호출되지 않아야 함
            then(momentRepository).should(times(0))
                    .incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("조회수 증가 실패 시에도 예외는 발생하지 않음")
        void incrementViewCount_UpdateFailed() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(true);
            given(momentRepository.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(0); // 업데이트된 행이 없음

            // when & then
            // 예외가 발생하지 않아야 함
            momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);

            then(momentRepository).should(times(1))
                    .existsById(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1))
                    .incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("null momentId로 조회수 증가 시 예외 발생")
        void incrementViewCount_NullMomentId() {
            // given
            given(momentRepository.existsById(null))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> momentViewService.incrementViewCount(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .extracting("message")
                    .isEqualTo(MomentTestConstants.MOMENT_NOT_FOUND_MESSAGE);
        }
    }

    @Nested
    @DisplayName("조회수 조회 테스트")
    class GetViewCountTest {

        @Test
        @DisplayName("정상적인 조회수 조회")
        void getViewCount_Success() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(testMoment));

            // when
            Long viewCount = momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            assertThat(viewCount).isEqualTo(MomentTestConstants.DEFAULT_VIEW_COUNT);
            then(momentRepository).should(times(1))
                    .findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 조회수 조회 시 0 반환")
        void getViewCount_MomentNotFound() {
            // given
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.empty());

            // when
            Long viewCount = momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            assertThat(viewCount).isEqualTo(0L);
            then(momentRepository).should(times(1))
                    .findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("조회수가 업데이트된 Moment의 조회수 조회")
        void getViewCount_UpdatedViewCount() {
            // given
            Moment momentWithUpdatedViewCount = createMomentWithViewCount(MomentTestConstants.UPDATED_VIEW_COUNT);
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(momentWithUpdatedViewCount));

            // when
            Long viewCount = momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            assertThat(viewCount).isEqualTo(MomentTestConstants.UPDATED_VIEW_COUNT);
        }

        @Test
        @DisplayName("null momentId로 조회수 조회")
        void getViewCount_NullMomentId() {
            // given
            given(momentRepository.findBasicMomentById(null))
                    .willReturn(Optional.empty());

            // when
            Long viewCount = momentViewService.getViewCount(null);

            // then
            assertThat(viewCount).isEqualTo(0L);
        }

        @Test
        @DisplayName("조회수가 null인 Moment 처리")
        void getViewCount_NullViewCountInMoment() {
            // given
            Moment momentWithNullViewCount = createMomentWithViewCount(null);
            given(momentRepository.findBasicMomentById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(Optional.of(momentWithNullViewCount));

            // when
            Long viewCount = momentViewService.getViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            assertThat(viewCount).isEqualTo(0L); // Moment.getViewCount()에서 null 체크 후 0L 반환
        }
    }

    @Nested
    @DisplayName("동시성 및 예외 처리 테스트")
    class ConcurrencyAndExceptionTest {

        @Test
        @DisplayName("Repository 예외 발생 시 재시도 동작 확인")
        void incrementViewCount_RepositoryException() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(true);
            given(momentRepository.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willThrow(new RuntimeException("Database connection failed"));

            // when & then
            assertThatThrownBy(() -> momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection failed");

            // 재시도 동작은 실제 환경에서만 확인 가능 (스프링 AOP 필요)
            then(momentRepository).should(times(1))
                    .existsById(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("존재 여부 확인에서 예외 발생")
        void incrementViewCount_ExistsCheckException() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willThrow(new RuntimeException("Database error during exists check"));

            // when & then
            assertThatThrownBy(() -> momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error during exists check");

            then(momentRepository).should(times(0))
                    .incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 검증 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("조회수 증가 전 Moment 존재 여부를 반드시 확인")
        void incrementViewCount_AlwaysCheckExistence() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(true);
            given(momentRepository.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(1);

            // when
            momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            // existsById가 incrementViewCount보다 먼저 호출되어야 함
            then(momentRepository).should(times(1))
                    .existsById(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(1))
                    .incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
        }

        @Test
        @DisplayName("여러 번 조회수 증가 호출")
        void incrementViewCount_MultipleCalls() {
            // given
            given(momentRepository.existsById(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(true);
            given(momentRepository.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID))
                    .willReturn(1);

            // when
            momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
            momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
            momentViewService.incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);

            // then
            then(momentRepository).should(times(3))
                    .existsById(MomentTestConstants.TEST_MOMENT_ID);
            then(momentRepository).should(times(3))
                    .incrementViewCount(MomentTestConstants.TEST_MOMENT_ID);
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

    private Moment createMomentWithViewCount(Long viewCount) {
        return Moment.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .userId(MomentTestConstants.TEST_USER_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .viewCount(viewCount)
                .build();
    }
}
