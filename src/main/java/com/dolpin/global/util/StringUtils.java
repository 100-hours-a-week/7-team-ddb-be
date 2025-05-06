package com.dolpin.global.util;

public class StringUtils {


    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isBlank(String str) {
        return !isNotBlank(str);
    }

}