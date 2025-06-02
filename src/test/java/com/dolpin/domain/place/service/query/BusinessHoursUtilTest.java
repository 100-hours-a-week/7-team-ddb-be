package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.PlaceHours;
import com.dolpin.global.constants.TestConstants;
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

            Method formatDistanceMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("formatDistance", Double.class);

            formatDistanceMethod.setAccessible(true);

            String result = (String) formatDistanceMethod.invoke(placeQueryService, distance);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("formatDistance null 입력 시 기본값을 반환한다")
        void formatDistance_WithNull_ReturnsDefault() throws Exception {

            Method formatDistanceMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("formatDistance", Double.class);
            formatDistanceMethod.setAccessible(true);

            String result = (String) formatDistanceMethod.invoke(placeQueryService, (Double) null);

            assertThat(result).isEqualTo(TestConstants.DISTANCE_ZERO);
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

            Method parseTimeToMinutesMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("parseTimeToMinutes", String.class);
            parseTimeToMinutesMethod.setAccessible(true);

            int result = (int) parseTimeToMinutesMethod.invoke(placeQueryService, timeString);

            assertThat(result).isEqualTo(expectedMinutes);
        }

        @Test
        @DisplayName("parseTimeToMinutes 잘못된 형식 입력 시 0을 반환한다")
        void parseTimeToMinutes_WithInvalidFormat_ReturnsZero() throws Exception {

            Method parseTimeToMinutesMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("parseTimeToMinutes", String.class);
            parseTimeToMinutesMethod.setAccessible(true);

            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, TestConstants.INVALID_TIME_FORMAT)).isEqualTo(0);
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, TestConstants.INVALID_TIME_HOUR)).isEqualTo(0);
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, TestConstants.EMPTY_STRING)).isEqualTo(0);
            assertThat((int) parseTimeToMinutesMethod.invoke(placeQueryService, (String) null)).isEqualTo(0);
        }

        @Test
        @DisplayName("buildDaySchedules 요일별 스케줄 구성이 정상 동작한다")
        void buildDaySchedules_BuildsSchedulesCorrectly() throws Exception {
            List<PlaceHours> placeHours = createCompleteBusinessHours();

            Method buildDaySchedulesMethod = PlaceQueryServiceImpl.class.getDeclaredMethod("buildDaySchedules", List.class);
            buildDaySchedulesMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<PlaceDetailResponse.Schedule> result = (List<PlaceDetailResponse.Schedule>)
                    buildDaySchedulesMethod.invoke(placeQueryService, placeHours);

            assertThat(result).hasSize(7);

            Optional<PlaceDetailResponse.Schedule> monday = result.stream()
                    .filter(s -> "mon".equals(s.getDay())).findFirst();
            assertThat(monday).isPresent();
            assertThat(monday.get().getHours()).isEqualTo(TestConstants.OPEN_TIME + "~" + TestConstants.CLOSE_TIME);
            assertThat(monday.get().getBreakTime()).isEqualTo(TestConstants.BREAK_START_TIME + "~" + TestConstants.BREAK_END_TIME);

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

            Method determineBusinessStatusMethod = PlaceQueryServiceImpl.class
                    .getDeclaredMethod("determineBusinessStatus", List.class);
            determineBusinessStatusMethod.setAccessible(true);


            String result = (String) determineBusinessStatusMethod.invoke(placeQueryService, hours);


            assertThat(result).isEqualTo(expectedStatus);
        }

        @Test
        @DisplayName("determineBusinessStatus 영업 중 반환 테스트")
        void determineBusinessStatus_WhenWithinOpenHours_ReturnsOpen() throws Exception {

            ZonedDateTime now = ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"));
            PlaceHours todayHours = createMockHours(TestConstants.MONDAY, TestConstants.FULL_DAY_START, TestConstants.FULL_DAY_END, false);

            Method m = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            m.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(now);
                String status = (String) m.invoke(placeQueryService, List.of(todayHours));
                assertThat(status).isEqualTo(TestConstants.BUSINESS_STATUS_OPEN);
            }
        }

        @Test
        @DisplayName("determineBusinessStatus 브레이크 타임 반환 테스트")
        void determineBusinessStatus_WhenDuringBreakTime_ReturnsBreak() throws Exception {
            ZonedDateTime now = ZonedDateTime.of(2025, 5, 26, 12, 30, 0, 0, ZoneId.of("Asia/Seoul"));

            PlaceHours openSlot = createMockHours(TestConstants.MONDAY, TestConstants.FULL_DAY_START, TestConstants.FULL_DAY_END, false);
            PlaceHours breakSlot = createMockHours(TestConstants.MONDAY, TestConstants.LUNCH_BREAK_START, TestConstants.LUNCH_BREAK_END, true);

            Method m = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            m.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(now);
                String status = (String) m.invoke(placeQueryService, List.of(openSlot, breakSlot));
                assertThat(status).isEqualTo(TestConstants.BUSINESS_STATUS_BREAK);
            }
        }

        @Test
        @DisplayName("determineBusinessStatus 영업 종료 반환 테스트")
        void determineBusinessStatus_WhenAfterCloseTime_ReturnsClosed() throws Exception {
            ZonedDateTime now = ZonedDateTime.of(2025, 5, 26, 22, 0, 0, 0, ZoneId.of("Asia/Seoul"));

            PlaceHours earlyClose = createMockHours(TestConstants.MONDAY, TestConstants.OPEN_TIME, TestConstants.EARLY_CLOSE_TIME, false);

            Method m = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            m.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(now);
                String status = (String) m.invoke(placeQueryService, List.of(earlyClose));
                assertThat(status).isEqualTo(TestConstants.BUSINESS_STATUS_CLOSED);
            }
        }

        private static Stream<Arguments> businessStatusTestCases() {
            return Stream.of(
                    Arguments.of(Collections.emptyList(), TestConstants.BUSINESS_STATUS_UNKNOWN),
                    Arguments.of((List<PlaceHours>) null, TestConstants.BUSINESS_STATUS_UNKNOWN),
                    Arguments.of(createClosedDayHours(), TestConstants.BUSINESS_STATUS_HOLIDAY)
            );
        }

        private static List<PlaceHours> createClosedDayHours() {

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            String currentDay = switch (now.getDayOfWeek()) {
                case MONDAY -> TestConstants.MONDAY;
                case TUESDAY -> TestConstants.TUESDAY;
                case WEDNESDAY -> TestConstants.WEDNESDAY;
                case THURSDAY -> TestConstants.THURSDAY;
                case FRIDAY -> TestConstants.FRIDAY;
                case SATURDAY -> TestConstants.SATURDAY;
                case SUNDAY -> TestConstants.SUNDAY;
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

            ZonedDateTime lateNight = ZonedDateTime.of(2025, 5, 26, 1, 30, 0, 0, ZoneId.of("Asia/Seoul"));

            PlaceHours mondayEarlyHours = createMockHours(TestConstants.MONDAY, TestConstants.TWENTY_FOUR_HOUR_START, TestConstants.LATE_NIGHT_CLOSE, false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(lateNight);

                String status = (String) method.invoke(placeQueryService, List.of(mondayEarlyHours));

                assertThat(status).isEqualTo(TestConstants.BUSINESS_STATUS_OPEN);
            }
        }

        @Test
        @DisplayName("자정을 넘어가는 영업시간 - 영업 종료 상태")
        void determineBusinessStatus_WithMidnightCrossing_WhenClosed_ReturnsClosed() throws Exception {

            ZonedDateTime earlyMorning = ZonedDateTime.of(2025, 5, 26, 3, 0, 0, 0, ZoneId.of("Asia/Seoul"));

            PlaceHours mondayEarlyHours = createMockHours(TestConstants.MONDAY, TestConstants.TWENTY_FOUR_HOUR_START, TestConstants.LATE_NIGHT_CLOSE, false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(earlyMorning);

                String status = (String) method.invoke(placeQueryService, List.of(mondayEarlyHours));

                assertThat(status).isEqualTo(TestConstants.BUSINESS_STATUS_CLOSED);
            }
        }

        @Test
        @DisplayName("24시간 운영 - 항상 영업 중")
        void determineBusinessStatus_With24HourOperation_AlwaysReturnsOpen() throws Exception {

            List<ZonedDateTime> testTimes = List.of(
                    ZonedDateTime.of(2025, 5, 26, 3, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                    ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                    ZonedDateTime.of(2025, 5, 26, 18, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                    ZonedDateTime.of(2025, 5, 26, 23, 59, 0, 0, ZoneId.of("Asia/Seoul"))
            );

            PlaceHours twentyFourHours = createMockHours(TestConstants.MONDAY, TestConstants.TWENTY_FOUR_HOUR_START, TestConstants.TWENTY_FOUR_HOUR_END, false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);

            for (ZonedDateTime testTime : testTimes) {
                try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                    mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(testTime);

                    String status = (String) method.invoke(placeQueryService, List.of(twentyFourHours));

                    assertThat(status).as("시간 %s에서 24시간 운영 상태 확인", testTime.toLocalTime())
                            .isEqualTo(TestConstants.BUSINESS_STATUS_OPEN);
                }
            }
        }

        @Test
        @DisplayName("경계 시간 테스트 - 오픈/마감 시각 정확한 처리")
        void determineBusinessStatus_AtBoundaryTimes_HandlesExactly() throws Exception {

            PlaceHours normalHours = createMockHours(TestConstants.MONDAY, TestConstants.OPEN_TIME, "18:00", false);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);


            Map<ZonedDateTime, String> boundaryTests = Map.of(

                    ZonedDateTime.of(2025, 5, 26, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_OPEN,

                    ZonedDateTime.of(2025, 5, 26, 8, 59, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_CLOSED,

                    ZonedDateTime.of(2025, 5, 26, 17, 59, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_OPEN,

                    ZonedDateTime.of(2025, 5, 26, 18, 0, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_CLOSED,

                    ZonedDateTime.of(2025, 5, 26, 18, 1, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_CLOSED
            );

            for (Map.Entry<ZonedDateTime, String> test : boundaryTests.entrySet()) {
                ZonedDateTime time = test.getKey();
                String expected = test.getValue();

                try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                    mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(time);

                    String status = (String) method.invoke(placeQueryService, List.of(normalHours));

                    assertThat(status).as("시각 %s에서의 영업 상태", time.toLocalTime())
                            .isEqualTo(expected);
                }
            }
        }

        @Test
        @DisplayName("브레이크 타임 경계 시간 테스트")
        void determineBusinessStatus_AtBreakTimeBoundaries_HandlesExactly() throws Exception {

            PlaceHours normalHours = createMockHours(TestConstants.MONDAY, TestConstants.OPEN_TIME, "18:00", false);


            PlaceHours breakHours = createMockHours(TestConstants.MONDAY, TestConstants.LUNCH_BREAK_START, TestConstants.LUNCH_BREAK_END, true);

            Method method = PlaceQueryServiceImpl.class.getDeclaredMethod("determineBusinessStatus", List.class);
            method.setAccessible(true);


            Map<ZonedDateTime, String> breakBoundaryTests = Map.of(

                    ZonedDateTime.of(2025, 5, 26, 11, 59, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_OPEN,

                    ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_BREAK,

                    ZonedDateTime.of(2025, 5, 26, 12, 30, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_BREAK,

                    ZonedDateTime.of(2025, 5, 26, 12, 59, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_BREAK,

                    ZonedDateTime.of(2025, 5, 26, 13, 0, 0, 0, ZoneId.of("Asia/Seoul")), TestConstants.BUSINESS_STATUS_OPEN
            );

            for (Map.Entry<ZonedDateTime, String> test : breakBoundaryTests.entrySet()) {
                ZonedDateTime time = test.getKey();
                String expected = test.getValue();

                try (MockedStatic<ZonedDateTime> mockNow = Mockito.mockStatic(ZonedDateTime.class)) {
                    mockNow.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(time);

                    String status = (String) method.invoke(placeQueryService, List.of(normalHours, breakHours));

                    assertThat(status).as("시각 %s에서의 브레이크 타임 상태", time.toLocalTime())
                            .isEqualTo(expected);
                }
            }
        }
    }
}
