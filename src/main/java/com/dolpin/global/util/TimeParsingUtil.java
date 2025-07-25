package com.dolpin.global.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Slf4j
public class TimeParsingUtil {

    private TimeParsingUtil() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }


    public static LocalTime parseTimeString(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            throw new IllegalArgumentException("시간 문자열이 null이거나 비어있습니다.");
        }

        String normalizedTime = normalizeTimeString(timeString.trim());

        try {
            return LocalTime.parse(normalizedTime);
        } catch (DateTimeParseException e) {
            log.error("시간 파싱 실패: timeString={}, normalizedTime={}", timeString, normalizedTime, e);
            throw new IllegalArgumentException("유효하지 않은 시간 형식입니다: " + timeString, e);
        }
    }


    private static String normalizeTimeString(String timeString) {
        // 24:00을 23:59로 변환 (자정을 의미하지만 LocalTime에서는 23:59로 처리)
        if ("24:00".equals(timeString)) {
            log.debug("24:00을 23:59로 정규화 처리");
            return "23:59";
        }

        // 다른 특수 케이스들도 필요시 여기에 추가
        return timeString;
    }


    public static boolean isValidTimeString(String timeString) {
        try {
            parseTimeString(timeString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
