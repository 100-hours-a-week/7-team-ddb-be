package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.PlaceHours;
import com.dolpin.global.constants.PlaceTestConstants;
import com.dolpin.global.util.BusinessTimeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("영업시간 및 유틸리티 메서드 테스트")
class BusinessHoursUtilTest {

    private MockedStatic<ZonedDateTime> mockedDateTime;

    @AfterEach
    void tearDown() {
        if (mockedDateTime != null) {
            mockedDateTime.close();
        }
    }

    @Nested
    @DisplayName("BusinessTimeUtil 클래스 테스트")
    class BusinessTimeUtilTest {

        @ParameterizedTest
        @MethodSource("distanceFormatTestCases")
        @DisplayName("거리 포맷팅이 정상 동작한다")
        void formatDistance_FormatsDistanceCorrectly(double distance, String expected) {
            String result = BusinessTimeUtil.formatDistance(distance);

            assertThat(result).isEqualTo(expected);
        }

        private static Stream<Arguments> distanceFormatTestCases() {
            return Stream.of(
                    Arguments.of(PlaceTestConstants.Distance.DISTANCE_500M_DOUBLE, PlaceTestConstants.Distance.DISTANCE_500M_TEXT),
                    Arguments.of(PlaceTestConstants.Distance.DISTANCE_999M_DOUBLE, PlaceTestConstants.Distance.DISTANCE_1000M_TEXT),
                    Arguments.of(PlaceTestConstants.Distance.DISTANCE_1000M_DOUBLE, PlaceTestConstants.Distance.DISTANCE_1KM_TEXT),
                    Arguments.of(PlaceTestConstants.Distance.DISTANCE_1500M_DOUBLE, PlaceTestConstants.Distance.DISTANCE_1_5KM_TEXT),
                    Arguments.of(PlaceTestConstants.Distance.DISTANCE_2340M_DOUBLE, PlaceTestConstants.Distance.DISTANCE_2_3KM_TEXT),
                    Arguments.of(PlaceTestConstants.Distance.DISTANCE_10000M_DOUBLE, PlaceTestConstants.Distance.DISTANCE_10KM_TEXT)
            );
        }

        @Test
        @DisplayName("formatDistance null 입력 시 기본값을 반환한다")
        void formatDistance_WithNull_ReturnsDefault() {
            String result = BusinessTimeUtil.formatDistance(null);

            assertThat(result).isEqualTo(PlaceTestConstants.DISTANCE_ZERO_TEXT);
        }

        @ParameterizedTest
        @MethodSource("timeParsingTestCases")
        @DisplayName("시간 파싱이 정상 동작한다")
        void parseTimeToMinutes_ParsesTimeCorrectly(String timeString, int expectedMinutes) {
            int result = BusinessTimeUtil.parseTimeToMinutes(timeString);

            assertThat(result).isEqualTo(expectedMinutes);
        }

        private static Stream<Arguments> timeParsingTestCases() {
            return Stream.of(
                    Arguments.of(PlaceTestConstants.Time.TIME_09_30, PlaceTestConstants.Time.MINUTES_570),
                    Arguments.of(PlaceTestConstants.Time.TIME_12_00, PlaceTestConstants.Time.MINUTES_720),
                    Arguments.of(PlaceTestConstants.Time.TIME_18_45, PlaceTestConstants.Time.MINUTES_1125),
                    Arguments.of(PlaceTestConstants.Time.TIME_23_59, PlaceTestConstants.Time.MINUTES_1439),
                    Arguments.of(PlaceTestConstants.Time.TIME_00_00, PlaceTestConstants.Time.MINUTES_0)
            );
        }

        @Test
        @DisplayName("buildDaySchedules 요일별 스케줄 구성이 정상 동작한다")
        void buildDaySchedules_BuildsSchedulesCorrectly() {
            List<PlaceHours> placeHours = createCompleteBusinessHours();

            List<PlaceDetailResponse.Schedule> result = BusinessTimeUtil.buildDaySchedules(placeHours);

            assertThat(result).hasSize(PlaceTestConstants.EXPECTED_WEEKDAYS_COUNT);

            Optional<PlaceDetailResponse.Schedule> monday = result.stream()
                    .filter(s -> "mon".equals(s.getDay())).findFirst();
            assertThat(monday).isPresent();
            assertThat(monday.get().getHours()).isEqualTo(PlaceTestConstants.OPEN_TIME + "~" + PlaceTestConstants.CLOSE_TIME);
            assertThat(monday.get().getBreakTime()).isEqualTo(PlaceTestConstants.BREAK_START_TIME + "~" + PlaceTestConstants.BREAK_END_TIME);

            Optional<PlaceDetailResponse.Schedule> sunday = result.stream()
                    .filter(s -> "sun".equals(s.getDay())).findFirst();
            assertThat(sunday).isPresent();
            assertThat(sunday.get().getHours()).isNull();
            assertThat(sunday.get().getBreakTime()).isNull();
        }

        @ParameterizedTest
        @MethodSource("businessStatusTestCases")
        @DisplayName("determineBusinessStatus 기본 케이스 정상 동작한다")
        void determineBusinessStatus_BasicCases_WorkCorrectly(
                List<PlaceHours> hours, String expectedStatus) {
            String result = BusinessTimeUtil.determineBusinessStatus(hours);

            assertThat(result).isEqualTo(expectedStatus);
        }

        private static Stream<Arguments> businessStatusTestCases() {
            return Stream.of(
                    Arguments.of(Collections.emptyList(), PlaceTestConstants.BUSINESS_STATUS_UNKNOWN),
                    Arguments.of(null, PlaceTestConstants.BUSINESS_STATUS_UNKNOWN),
                    Arguments.of(createClosedDayHours(), PlaceTestConstants.BUSINESS_STATUS_HOLIDAY)
            );
        }

        @Test
        @DisplayName("determineBusinessStatus 영업 중 반환 테스트")
        void determineBusinessStatus_WhenWithinOpenHours_ReturnsOpen() {
            ZonedDateTime testTime = ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")); // 월요일 12시
            PlaceHours todayHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.FULL_DAY_START, PlaceTestConstants.FULL_DAY_END, false);

            mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
            mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(testTime);
            String status = BusinessTimeUtil.determineBusinessStatus(List.of(todayHours));

            assertThat(status).isEqualTo(PlaceTestConstants.BUSINESS_STATUS_OPEN);
        }

        @Test
        @DisplayName("determineBusinessStatus 브레이크 타임 반환 테스트")
        void determineBusinessStatus_WhenDuringBreakTime_ReturnsBreak() {
            ZonedDateTime testTime = ZonedDateTime.of(2025, 5, 26, 12, 30, 0, 0, ZoneId.of("Asia/Seoul")); // 월요일 12:30

            PlaceHours openSlot = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.FULL_DAY_START, PlaceTestConstants.FULL_DAY_END, false);
            PlaceHours breakSlot = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.LUNCH_BREAK_START, PlaceTestConstants.LUNCH_BREAK_END, true);

            mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
            mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(testTime);
            String status = BusinessTimeUtil.determineBusinessStatus(List.of(openSlot, breakSlot));

            assertThat(status).isEqualTo(PlaceTestConstants.BUSINESS_STATUS_BREAK);
        }

        @Test
        @DisplayName("determineBusinessStatus 영업 종료 반환 테스트")
        void determineBusinessStatus_WhenAfterCloseTime_ReturnsClosed() {
            ZonedDateTime testTime = ZonedDateTime.of(2025, 5, 26, 22, 0, 0, 0, ZoneId.of("Asia/Seoul")); // 월요일 22시
            PlaceHours earlyClose = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.EARLY_CLOSE_TIME, false);

            mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
            mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(testTime);
            String status = BusinessTimeUtil.determineBusinessStatus(List.of(earlyClose));

            assertThat(status).isEqualTo(PlaceTestConstants.BUSINESS_STATUS_CLOSED);
        }
    }

    @Nested
    @DisplayName("복잡한 영업시간 엣지 케이스 테스트")
    class ComplexBusinessHoursTest {

        @Test
        @DisplayName("자정을 넘어가는 영업시간 - 영업 중 상태")
        void determineBusinessStatus_WithMidnightCrossing_WhenOpen_ReturnsOpen() {
            ZonedDateTime lateNight = ZonedDateTime.of(2025, 5, 26, 1, 30, 0, 0, ZoneId.of("Asia/Seoul"));
            PlaceHours mondayEarlyHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.TWENTY_FOUR_HOUR_START, PlaceTestConstants.LATE_NIGHT_CLOSE, false);

            mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
            mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(lateNight);
            String status = BusinessTimeUtil.determineBusinessStatus(List.of(mondayEarlyHours));

            assertThat(status).isEqualTo(PlaceTestConstants.BUSINESS_STATUS_OPEN);
        }

        @Test
        @DisplayName("자정을 넘어가는 영업시간 - 영업 종료 상태")
        void determineBusinessStatus_WithMidnightCrossing_WhenClosed_ReturnsClosed() {
            ZonedDateTime earlyMorning = ZonedDateTime.of(2025, 5, 26, 3, 0, 0, 0, ZoneId.of("Asia/Seoul"));
            PlaceHours mondayEarlyHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.TWENTY_FOUR_HOUR_START, PlaceTestConstants.LATE_NIGHT_CLOSE, false);

            mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
            mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(earlyMorning);
            String status = BusinessTimeUtil.determineBusinessStatus(List.of(mondayEarlyHours));

            assertThat(status).isEqualTo(PlaceTestConstants.BUSINESS_STATUS_CLOSED);
        }

        @Test
        @DisplayName("24시간 운영 - 항상 영업 중")
        void determineBusinessStatus_With24HourOperation_AlwaysReturnsOpen() {
            List<ZonedDateTime> testTimes = List.of(
                    ZonedDateTime.of(2025, 5, 26, 3, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                    ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                    ZonedDateTime.of(2025, 5, 26, 18, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                    ZonedDateTime.of(2025, 5, 26, 23, 59, 0, 0, ZoneId.of("Asia/Seoul"))
            );

            PlaceHours twentyFourHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.TWENTY_FOUR_HOUR_START, PlaceTestConstants.TWENTY_FOUR_HOUR_END, false);

            for (ZonedDateTime testTime : testTimes) {
                if (mockedDateTime != null) {
                    mockedDateTime.close();
                }
                mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
                mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(testTime);
                String status = BusinessTimeUtil.determineBusinessStatus(List.of(twentyFourHours));

                assertThat(status)
                        .as("시간 %s에서 24시간 운영 상태 확인", testTime.toLocalTime())
                        .isEqualTo(PlaceTestConstants.BUSINESS_STATUS_OPEN);
            }
        }

        @Test
        @DisplayName("경계 시간 테스트 - 오픈/마감 시각 정확한 처리")
        void determineBusinessStatus_AtBoundaryTimes_HandlesExactly() {
            PlaceHours normalHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.TEST_CLOSE_TIME_18, false);

            Map<ZonedDateTime, String> boundaryTests = Map.of(
                    ZonedDateTime.of(2025, 5, 26, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_OPEN,
                    ZonedDateTime.of(2025, 5, 26, 8, 59, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_CLOSED,
                    ZonedDateTime.of(2025, 5, 26, 17, 59, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_OPEN,
                    ZonedDateTime.of(2025, 5, 26, 18, 0, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_CLOSED,
                    ZonedDateTime.of(2025, 5, 26, 18, 1, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_CLOSED
            );

            for (Map.Entry<ZonedDateTime, String> test : boundaryTests.entrySet()) {
                ZonedDateTime time = test.getKey();
                String expected = test.getValue();

                if (mockedDateTime != null) {
                    mockedDateTime.close();
                }
                mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
                mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(time);
                String status = BusinessTimeUtil.determineBusinessStatus(List.of(normalHours));

                assertThat(status)
                        .as("시각 %s에서의 영업 상태", time.toLocalTime())
                        .isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("브레이크 타임 경계 시간 테스트")
        void determineBusinessStatus_AtBreakTimeBoundaries_HandlesExactly() {
            PlaceHours normalHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.TEST_CLOSE_TIME_18, false);
            PlaceHours breakHours = createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.LUNCH_BREAK_START, PlaceTestConstants.LUNCH_BREAK_END, true);

            Map<ZonedDateTime, String> breakBoundaryTests = Map.of(
                    ZonedDateTime.of(2025, 5, 26, 11, 59, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_OPEN,
                    ZonedDateTime.of(2025, 5, 26, 12, 0, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_BREAK,
                    ZonedDateTime.of(2025, 5, 26, 12, 30, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_BREAK,
                    ZonedDateTime.of(2025, 5, 26, 12, 59, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_BREAK,
                    ZonedDateTime.of(2025, 5, 26, 13, 0, 0, 0, ZoneId.of("Asia/Seoul")), PlaceTestConstants.BUSINESS_STATUS_OPEN
            );

            for (Map.Entry<ZonedDateTime, String> test : breakBoundaryTests.entrySet()) {
                ZonedDateTime time = test.getKey();
                String expected = test.getValue();

                if (mockedDateTime != null) {
                    mockedDateTime.close();
                }
                mockedDateTime = Mockito.mockStatic(ZonedDateTime.class);
                mockedDateTime.when(() -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"))).thenReturn(time);
                String status = BusinessTimeUtil.determineBusinessStatus(List.of(normalHours, breakHours));

                assertThat(status)
                        .as("시각 %s에서의 브레이크 타임 상태", time.toLocalTime())
                        .isEqualTo(expected);
            }
        }
    }

    private static PlaceHours createPlaceHours(String day, String openTime, String closeTime, Boolean isBreakTime) {
        return PlaceHours.builder()
                .dayOfWeek(day)
                .openTime(openTime)
                .closeTime(closeTime)
                .isBreakTime(isBreakTime)
                .build();
    }

    private static List<PlaceHours> createCompleteBusinessHours() {
        List<PlaceHours> hours = new ArrayList<>();

        // 월요일 - 정규 영업시간 + 브레이크 타임
        hours.add(createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.CLOSE_TIME, false));
        hours.add(createPlaceHours(PlaceTestConstants.MONDAY, PlaceTestConstants.BREAK_START_TIME, PlaceTestConstants.BREAK_END_TIME, true));

        // 화~토 - 정규 영업시간만
        String[] workingDays = {PlaceTestConstants.TUESDAY, PlaceTestConstants.WEDNESDAY, PlaceTestConstants.THURSDAY, PlaceTestConstants.FRIDAY, PlaceTestConstants.SATURDAY};
        for (String day : workingDays) {
            hours.add(createPlaceHours(day, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.CLOSE_TIME, false));
        }

        // 일요일 - 휴무
        hours.add(createPlaceHours(PlaceTestConstants.SUNDAY, null, null, false));

        return hours;
    }

    private static List<PlaceHours> createClosedDayHours() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        String currentDay = switch (now.getDayOfWeek()) {
            case MONDAY -> PlaceTestConstants.MONDAY;
            case TUESDAY -> PlaceTestConstants.TUESDAY;
            case WEDNESDAY -> PlaceTestConstants.WEDNESDAY;
            case THURSDAY -> PlaceTestConstants.THURSDAY;
            case FRIDAY -> PlaceTestConstants.FRIDAY;
            case SATURDAY -> PlaceTestConstants.SATURDAY;
            case SUNDAY -> PlaceTestConstants.SUNDAY;
        };

        return List.of(createPlaceHours(currentDay, null, null, false));
    }
}
