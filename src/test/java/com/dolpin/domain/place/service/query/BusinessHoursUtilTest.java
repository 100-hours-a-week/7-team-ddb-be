package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.PlaceHours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.dolpin.global.helper.PlaceTestHelper.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("영업시간 및 유틸리티 메서드 테스트")
class BusinessHoursUtilTest {

    private final PlaceQueryServiceImpl placeQueryService = new PlaceQueryServiceImpl(null, null);

    @Nested
    @DisplayName("Private 메서드 간접 테스트")
    class PrivateMethodsTest {

        @ParameterizedTest
        @CsvSource({
                "500.0, 500m",
                "999.9, 1000m",
                "1000.0, 1.0km",
                "1500.0, 1.5km",
                "2340.7, 2.3km",
                "10000.0, 10.0km"
        })
        @DisplayName("formatDistance 거리 포맷팅이 정상 동작한다")
        void formatDistance_FormatsDistanceCorrectly(double distance, String expected) throws Exception {
            // given
            Method formatDistanceMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("formatDistance", Double.class);
            formatDistanceMethod.setAccessible(true);

            // when
            String result = (String) formatDistanceMethod.invoke(placeQueryService, distance);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("formatDistance null 입력 시 0을 반환한다")
        void formatDistance_WithNull_ReturnsZero() throws Exception {
            // given
            Method formatDistanceMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("formatDistance", Double.class);
            formatDistanceMethod.setAccessible(true);

            // when
            String result = (String) formatDistanceMethod.invoke(placeQueryService, (Double) null);

            // then
            assertThat(result).isEqualTo("0");
        }

        @ParameterizedTest
        @CsvSource({
                "09:30, 570",
                "12:00, 720",
                "18:45, 1125",
                "23:59, 1439",
                "00:00, 0"
        })
        @DisplayName("parseTimeToMinutes 시간 파싱이 정상 동작한다")
        void parseTimeToMinutes_ParsesTimeCorrectly(String timeString, int expectedMinutes) throws Exception {
            // given
            Method parseTimeToMinutesMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("parseTimeToMinutes", String.class);
            parseTimeToMinutesMethod.setAccessible(true);

            // when
            int result = (int) parseTimeToMinutesMethod.invoke(placeQueryService, timeString);

            // then
            assertThat(result).isEqualTo(expectedMinutes);
        }

        @Test
        @DisplayName("parseTimeToMinutes 잘못된 형식 입력 시 0을 반환한다")
        void parseTimeToMinutes_WithInvalidFormat_ReturnsZero() throws Exception {
            // given
            Method parseTimeToMinutesMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("parseTimeToMinutes", String.class);
            parseTimeToMinutesMethod.setAccessible(true);

            // when & then
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, "invalid")).isEqualTo(0);
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, "25:00")).isEqualTo(0);
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, "")).isEqualTo(0);
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, (String) null)).isEqualTo(0);
        }

        @Test
        @DisplayName("buildDaySchedules 요일별 스케줄 구성이 정상 동작한다")
        void buildDaySchedules_BuildsSchedulesCorrectly() throws Exception {
            // given
            List<PlaceHours> placeHours = createCompleteBusinessHours();

            Method buildDaySchedulesMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("buildDaySchedules", List.class);
            buildDaySchedulesMethod.setAccessible(true);

            // when
            @SuppressWarnings("unchecked")
            List<PlaceDetailResponse.Schedule> result = (List<PlaceDetailResponse.Schedule>)
                    buildDaySchedulesMethod.invoke(placeQueryService, placeHours);

            // then
            assertThat(result).hasSize(7); // 7일

            // 월요일 확인 (영업 + 브레이크타임)
            Optional<PlaceDetailResponse.Schedule> monday = result.stream()
                    .filter(s -> "mon".equals(s.getDay())).findFirst();
            assertThat(monday).isPresent();
            assertThat(monday.get().getHours()).isEqualTo("09:00~21:00");
            assertThat(monday.get().getBreakTime()).isEqualTo("15:00~16:00");

            // 일요일 확인 (휴무일)
            Optional<PlaceDetailResponse.Schedule> sunday = result.stream()
                    .filter(s -> "sun".equals(s.getDay())).findFirst();
            assertThat(sunday).isPresent();
            assertThat(sunday.get().getHours()).isNull();
            assertThat(sunday.get().getBreakTime()).isNull();
        }

        @ParameterizedTest
        @MethodSource("businessStatusTestCases")
        @DisplayName("determineBusinessStatus 영업 상태 판단이 정상 동작한다")
        void determineBusinessStatus_DeterminesStatusCorrectly(
                List<PlaceHours> hours, String expectedStatus) throws Exception {
            // given
            Method determineBusinessStatusMethod = PlaceQueryServiceImpl.class
                    .getDeclaredMethod("determineBusinessStatus", List.class);
            determineBusinessStatusMethod.setAccessible(true);

            // when
            String result = (String) determineBusinessStatusMethod.invoke(placeQueryService, hours);

            // then
            assertThat(result).isEqualTo(expectedStatus);
        }

        @Test
        @DisplayName("determineBusinessStatus 영업 중 반환 테스트")
        void determineBusinessStatus_WhenWithinOpenHours_ReturnsOpen() throws Exception {
            // 2025-05-26 12:00 서울 기준
            ZonedDateTime now = ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"));
            PlaceHours todayHours = createMockHours("월", "00:00", "23:59", false);

            Method m = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            m.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(now);
                String status = (String) m.invoke(placeQueryService, List.of(todayHours));
                assertThat(status).isEqualTo("영업 중");
            }
        }

        @Test
        @DisplayName("determineBusinessStatus 브레이크 타임 반환 테스트")
        void determineBusinessStatus_WhenDuringBreakTime_ReturnsBreak() throws Exception {
            ZonedDateTime now = ZonedDateTime.of(2025, 5, 26, 12, 30, 0, 0, ZoneId.of("Asia/Seoul"));
            String day = "월";

            PlaceHours openSlot = createMockHours(day, "00:00", "23:59", false);
            PlaceHours breakSlot = createMockHours(day, "12:00", "13:00", true);

            Method m = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            m.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(now);
                String status = (String) m.invoke(placeQueryService, List.of(openSlot, breakSlot));
                assertThat(status).isEqualTo("브레이크 타임");
            }
        }

        @Test
        @DisplayName("determineBusinessStatus 영업 종료 반환 테스트")
        void determineBusinessStatus_WhenAfterCloseTime_ReturnsClosed() throws Exception {
            ZonedDateTime now = ZonedDateTime.of(2025, 5, 26, 22, 0, 0, 0, ZoneId.of("Asia/Seoul"));
            String day = "월";

            PlaceHours earlyClose = createMockHours(day, "09:00", "20:00", false);

            Method m = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            m.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(now);
                String status = (String) m.invoke(placeQueryService, List.of(earlyClose));
                assertThat(status).isEqualTo("영업 종료");
            }
        }

        private static Stream<Arguments> businessStatusTestCases() {
            return Stream.of(
                    Arguments.of(Collections.emptyList(), "영업 여부 확인 필요"),
                    Arguments.of((List<PlaceHours>) null, "영업 여부 확인 필요"),
                    Arguments.of(createClosedDayHours(), "휴무일")
            );
        }

        private static List<PlaceHours> createClosedDayHours() {
            // 현재 요일로 설정 (테스트가 실행되는 시점의 요일)
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            String currentDay = switch (now.getDayOfWeek()) {
                case MONDAY -> "월";
                case TUESDAY -> "화";
                case WEDNESDAY -> "수";
                case THURSDAY -> "목";
                case FRIDAY -> "금";
                case SATURDAY -> "토";
                case SUNDAY -> "일";
            };

            PlaceHours closedHours = mock(PlaceHours.class);
            given(closedHours.getDayOfWeek()).willReturn(currentDay);
            given(closedHours.getIsBreakTime()).willReturn(false);
            given(closedHours.getOpenTime()).willReturn(null);
            given(closedHours.getCloseTime()).willReturn(null);

            return List.of(closedHours);
        }
    }

    @Nested
    @DisplayName("복잡한 영업시간 엣지 케이스 테스트")
    class ComplexBusinessHoursTest {

        @Test
        @DisplayName("자정을 넘어가는 영업시간 - 영업 중 상태")
        void determineBusinessStatus_WithMidnightCrossing_WhenOpen_ReturnsOpen() throws Exception {
            // 2025-05-26 01:30 (월요일 새벽)
            ZonedDateTime lateNight = ZonedDateTime.of(2025, 5, 26, 1, 30, 0, 0, ZoneId.of("Asia/Seoul"));

            // 월요일 00:00 ~ 02:00 영업 (자정 넘어가는 영업의 후반부)
            PlaceHours mondayEarlyHours = createMockHours("월", "00:00", "02:00", false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(lateNight);

                // when
                String status = (String) method.invoke(placeQueryService, List.of(mondayEarlyHours));

                // then
                assertThat(status).isEqualTo("영업 중");
            }
        }

        @Test
        @DisplayName("자정을 넘어가는 영업시간 - 영업 종료 상태")
        void determineBusinessStatus_WithMidnightCrossing_WhenClosed_ReturnsClosed() throws Exception {
            // 2025-05-26 03:00 (월요일 새벽)
            ZonedDateTime earlyMorning = ZonedDateTime.of(2025, 5, 26, 3, 0, 0, 0, ZoneId.of("Asia/Seoul"));

            // 월요일 00:00 ~ 02:00 영업 (이미 마감됨)
            PlaceHours mondayEarlyHours = createMockHours("월", "00:00", "02:00", false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(earlyMorning);

                // when
                String status = (String) method.invoke(placeQueryService, List.of(mondayEarlyHours));

                // then
                assertThat(status).isEqualTo("영업 종료");
            }
        }

        @Test
        @DisplayName("24시간 운영 - 항상 영업 중")
        void determineBusinessStatus_With24HourOperation_AlwaysReturnsOpen() throws Exception {
            // 다양한 시간대 테스트
            List<ZonedDateTime> testTimes = List.of(
                    ZonedDateTime.of(2025, 5, 26, 3, 0, 0, 0, ZoneId.of("Asia/Seoul")),   // 새벽 3시
                    ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")),  // 정오
                    ZonedDateTime.of(2025, 5, 26, 18, 0, 0, 0, ZoneId.of("Asia/Seoul")),  // 저녁 6시
                    ZonedDateTime.of(2025, 5, 26, 23, 59, 0, 0, ZoneId.of("Asia/Seoul"))  // 밤 11시 59분
            );

            // 24시간 운영 (00:00 ~ 00:00)
            PlaceHours twentyFourHours = createMockHours("월", "00:00", "00:00", false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            for (ZonedDateTime testTime : testTimes) {
                try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                    mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(testTime);

                    // when
                    String status = (String) method.invoke(placeQueryService, List.of(twentyFourHours));

                    // then
                    assertThat(status).as("시간 %s에서 24시간 운영 상태 확인", testTime.toLocalTime())
                            .isEqualTo("영업 중");
                }
            }
        }

        @Test
        @DisplayName("경계 시간 테스트 - 오픈/마감 시각 정확한 처리")
        void determineBusinessStatus_AtBoundaryTimes_HandlesExactly() throws Exception {
            String day = "월";

            // 09:00-18:00 영업
            PlaceHours normalHours = createMockHours(day, "09:00", "18:00", false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            // 테스트 케이스들
            Map<ZonedDateTime, String> boundaryTests = Map.of(
                    // 오픈 시각 정확히
                    ZonedDateTime.of(2025, 5, 26, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")), "영업 중",
                    // 오픈 1분 전
                    ZonedDateTime.of(2025, 5, 26, 8, 59, 0, 0, ZoneId.of("Asia/Seoul")), "영업 종료",
                    // 마감 1분 전
                    ZonedDateTime.of(2025, 5, 26, 17, 59, 0, 0, ZoneId.of("Asia/Seoul")), "영업 중",
                    // 마감 시각 정확히
                    ZonedDateTime.of(2025, 5, 26, 18, 0, 0, 0, ZoneId.of("Asia/Seoul")), "영업 종료",
                    // 마감 1분 후
                    ZonedDateTime.of(2025, 5, 26, 18, 1, 0, 0, ZoneId.of("Asia/Seoul")), "영업 종료"
            );

            for (Map.Entry<ZonedDateTime, String> test : boundaryTests.entrySet()) {
                ZonedDateTime time = test.getKey();
                String expected = test.getValue();

                try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                    mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(time);

                    // when
                    String status = (String) method.invoke(placeQueryService, List.of(normalHours));

                    // then
                    assertThat(status).as("시각 %s에서의 영업 상태", time.toLocalTime())
                            .isEqualTo(expected);
                }
            }
        }

        @Test
        @DisplayName("브레이크 타임 경계 시간 테스트")
        void determineBusinessStatus_AtBreakTimeBoundaries_HandlesExactly() throws Exception {
            String day = "월";

            // 09:00-18:00 영업
            PlaceHours normalHours = createMockHours(day, "09:00", "18:00", false);

            // 12:00-13:00 브레이크 타임
            PlaceHours breakHours = createMockHours(day, "12:00", "13:00", true);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            // 브레이크 타임 경계 테스트
            Map<ZonedDateTime, String> breakBoundaryTests = Map.of(
                    // 브레이크 타임 시작 1분 전
                    ZonedDateTime.of(2025, 5, 26, 11, 59, 0, 0, ZoneId.of("Asia/Seoul")), "영업 중",
                    // 브레이크 타임 시작 정확히
                    ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")), "브레이크 타임",
                    // 브레이크 타임 중간
                    ZonedDateTime.of(2025, 5, 26, 12, 30, 0, 0, ZoneId.of("Asia/Seoul")), "브레이크 타임",
                    // 브레이크 타임 끝 1분 전
                    ZonedDateTime.of(2025, 5, 26, 12, 59, 0, 0, ZoneId.of("Asia/Seoul")), "브레이크 타임",
                    // 브레이크 타임 끝 정확히
                    ZonedDateTime.of(2025, 5, 26, 13, 0, 0, 0, ZoneId.of("Asia/Seoul")), "영업 중"
            );

            for (Map.Entry<ZonedDateTime, String> test : breakBoundaryTests.entrySet()) {
                ZonedDateTime time = test.getKey();
                String expected = test.getValue();

                try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                    mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(time);

                    // when
                    String status = (String) method.invoke(placeQueryService, List.of(normalHours, breakHours));

                    // then
                    assertThat(status).as("시각 %s에서의 브레이크 타임 상태", time.toLocalTime())
                            .isEqualTo(expected);
                }
            }
        }
    }
}
