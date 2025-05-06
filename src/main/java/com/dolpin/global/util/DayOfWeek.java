package com.dolpin.global.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum DayOfWeek {
    MONDAY("mon", "월"),
    TUESDAY("tue", "화"),
    WEDNESDAY("wed", "수"),
    THURSDAY("thu", "목"),
    FRIDAY("fri", "금"),
    SATURDAY("sat", "토"),
    SUNDAY("sun", "일");

    private final String englishCode;
    private final String koreanCode;

    private static final Map<String, DayOfWeek> BY_ENGLISH_CODE =
            Arrays.stream(values()).collect(Collectors.toMap(DayOfWeek::getEnglishCode, Function.identity()));

    private static final Map<String, DayOfWeek> BY_KOREAN_CODE =
            Arrays.stream(values()).collect(Collectors.toMap(DayOfWeek::getKoreanCode, Function.identity()));

    public static DayOfWeek findByEnglishCode(String code) {
        return BY_ENGLISH_CODE.getOrDefault(code, null);
    }

    public static DayOfWeek findByKoreanCode(String code) {
        return BY_KOREAN_CODE.getOrDefault(code, null);
    }

    public static String getKoreanCodeByEnglishCode(String englishCode) {
        DayOfWeek dayOfWeek = findByEnglishCode(englishCode);
        return dayOfWeek != null ? dayOfWeek.getKoreanCode() : englishCode;
    }

    public static String getEnglishCodeByKoreanCode(String koreanCode) {
        DayOfWeek dayOfWeek = findByKoreanCode(koreanCode);
        return dayOfWeek != null ? dayOfWeek.getEnglishCode() : koreanCode;
    }
}