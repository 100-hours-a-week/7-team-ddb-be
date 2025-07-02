package com.dolpin.global.util;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.PlaceHours;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class BusinessTimeUtil {

    private BusinessTimeUtil() {
    }

    public static String formatDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return "0";

        if (distanceInMeters < 1000) {
            return Math.round(distanceInMeters) + "m";
        } else {
            return BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue() + "km";
        }
    }

    public static int parseTimeToMinutes(String timeString) {
        try {
            String[] parts = timeString.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 60 + minute;
        } catch (Exception e) {
            return 0;
        }
    }

    public static List<PlaceDetailResponse.Schedule> buildDaySchedules(List<PlaceHours> placeHours) {
        String[] dayCodesEn = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

        Map<String, Map<Boolean, PlaceHours>> hoursByDayAndType = new HashMap<>();

        for (PlaceHours hour : placeHours) {
            String englishDay = DayOfWeek.getEnglishCodeByKoreanCode(hour.getDayOfWeek());

            if (!hoursByDayAndType.containsKey(englishDay)) {
                hoursByDayAndType.put(englishDay, new HashMap<>());
            }

            hoursByDayAndType.get(englishDay).put(hour.getIsBreakTime(), hour);
        }

        List<PlaceDetailResponse.Schedule> schedules = new ArrayList<>();
        for (String dayCode : dayCodesEn) {
            Map<Boolean, PlaceHours> dayHoursMap = hoursByDayAndType.getOrDefault(dayCode, new HashMap<>());

            PlaceHours regularHours = dayHoursMap.get(false);
            PlaceHours breakHours = dayHoursMap.get(true);

            PlaceDetailResponse.Schedule.ScheduleBuilder builder =
                    PlaceDetailResponse.Schedule.builder().day(dayCode);

            if (regularHours != null && regularHours.getOpenTime() != null && regularHours.getCloseTime() != null) {
                builder.hours(regularHours.getOpenTime() + "~" + regularHours.getCloseTime());
            } else {
                builder.hours(null);
            }

            if (breakHours != null && breakHours.getOpenTime() != null && breakHours.getCloseTime() != null) {
                builder.breakTime(breakHours.getOpenTime() + "~" + breakHours.getCloseTime());
            } else {
                builder.breakTime(null);
            }

            schedules.add(builder.build());
        }

        return schedules;
    }

    public static String determineBusinessStatus(List<PlaceHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return "영업 여부 확인 필요";
        }

        var koreaZoneId = ZoneId.of("Asia/Seoul");
        var now = ZonedDateTime.now(koreaZoneId);

        var koreanDayOfWeek = switch (now.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };

        var currentHour = now.getHour();
        var currentMinute = now.getMinute();

        var todayRegularHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && !h.getIsBreakTime())
                .findFirst();

        var todayBreakHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && h.getIsBreakTime())
                .findFirst();

        if (todayRegularHours.isEmpty()) {
            return "영업 정보 없음";
        }

        var regular = todayRegularHours.get();

        if (regular.getOpenTime() == null || regular.getCloseTime() == null) {
            return "휴무일";
        }

        var currentTimeInMinutes = currentHour * 60 + currentMinute;
        var regularOpenTimeInMinutes = parseTimeToMinutes(regular.getOpenTime());
        var regularCloseTimeInMinutes = parseTimeToMinutes(regular.getCloseTime());

        record BreakTime(int start, int end) {}
        var breakTime = todayBreakHours
                .filter(b -> b.getOpenTime() != null && b.getCloseTime() != null)
                .map(b -> new BreakTime(
                        parseTimeToMinutes(b.getOpenTime()),
                        parseTimeToMinutes(b.getCloseTime())
                ));

        if (breakTime.isPresent() &&
                currentTimeInMinutes >= breakTime.get().start &&
                currentTimeInMinutes < breakTime.get().end) {
            return "브레이크 타임";
        }

        return regularOpenTimeInMinutes < regularCloseTimeInMinutes
                ? (currentTimeInMinutes >= regularOpenTimeInMinutes &&
                currentTimeInMinutes < regularCloseTimeInMinutes)
                ? "영업 중" : "영업 종료"
                : (currentTimeInMinutes >= regularOpenTimeInMinutes ||
                currentTimeInMinutes < regularCloseTimeInMinutes)
                ? "영업 중" : "영업 종료";
    }
}
