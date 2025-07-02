package com.dolpin.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtils {

    public static void logApiResponse(Logger logger, String endpoint, int statusCode,
                                      long duration, Long userId) {
        MDC.put("endpoint", endpoint);
        MDC.put("statusCode", String.valueOf(statusCode));
        if (userId != null) {
            MDC.put("userId", userId.toString());
        }

        try {
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("API response - endpoint: {}, status: {}, duration: {}ms",
                        endpoint, statusCode, duration);
            } else if (statusCode >= 400 && statusCode < 500) {
                logger.warn("API client error - endpoint: {}, status: {}, duration: {}ms",
                        endpoint, statusCode, duration);
            } else if (statusCode >= 500) {
                logger.error("API server error - endpoint: {}, status: {}, duration: {}ms",
                        endpoint, statusCode, duration);
            }
        } finally {
            MDC.remove("endpoint");
            MDC.remove("statusCode");
            if (userId != null) {
                MDC.remove("userId");
            }
        }
    }

}
