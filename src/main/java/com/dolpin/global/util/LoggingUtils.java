package com.dolpin.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtils {

    public static <T> T executeWithTiming(Logger logger, String operation, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Starting operation: {}", operation);
            T result = supplier.get();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Completed operation: {} in {}ms", operation, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Failed operation: {} after {}ms - {}", operation, duration, e.getMessage(), e);
            throw e;
        }
    }

    public static <T> T executeWithContext(Map<String, String> context, Supplier<T> supplier) {
        // 기존 MDC 값 백업
        Map<String, String> originalContext = MDC.getCopyOfContextMap();

        try {
            // 새로운 컨텍스트 추가
            context.forEach(MDC::put);
            return supplier.get();
        } finally {
            // 원래 컨텍스트 복원
            MDC.clear();
            if (originalContext != null) {
                MDC.setContextMap(originalContext);
            }
        }
    }

    public static void logUserAction(Logger logger, String action, Long userId, Map<String, Object> details) {
        MDC.put("userAction", action);
        try {
            logger.info("User action performed - action: {}, userId: {}, details: {}",
                    action, userId, details);
        } finally {
            MDC.remove("userAction");
        }
    }

    public static void logExternalServiceCall(Logger logger, String service, String operation,
                                              long duration, boolean success) {
        MDC.put("externalService", service);
        MDC.put("operation", operation);

        try {
            if (success) {
                logger.info("External service call succeeded - service: {}, operation: {}, duration: {}ms",
                        service, operation, duration);
            } else {
                logger.warn("External service call failed - service: {}, operation: {}, duration: {}ms",
                        service, operation, duration);
            }
        } finally {
            MDC.remove("externalService");
            MDC.remove("operation");
        }
    }


    public static void logSecurityEvent(Logger logger, String eventType, String details,
                                        String ipAddress, Long userId) {
        MDC.put("securityEvent", eventType);
        MDC.put("ipAddress", ipAddress);
        if (userId != null) {
            MDC.put("userId", userId.toString());
        }

        try {
            logger.warn("Security event - type: {}, details: {}, ip: {}, userId: {}",
                    eventType, details, ipAddress, userId);
        } finally {
            MDC.remove("securityEvent");
            MDC.remove("ipAddress");
            if (userId != null) {
                MDC.remove("userId");
            }
        }
    }

    public static void logDatabaseOperation(Logger logger, String operation, String table,
                                            int affectedRows, long duration) {
        MDC.put("dbOperation", operation);
        MDC.put("dbTable", table);

        try {
            logger.debug("Database operation - operation: {}, table: {}, affectedRows: {}, duration: {}ms",
                    operation, table, affectedRows, duration);
        } finally {
            MDC.remove("dbOperation");
            MDC.remove("dbTable");
        }
    }

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

    public static void logBusinessMetric(Logger logger, String metricName, Number value,
                                         Map<String, String> tags) {
        MDC.put("metric", metricName);
        tags.forEach(MDC::put);

        try {
            logger.info("Business metric - name: {}, value: {}, tags: {}",
                    metricName, value, tags);
        } finally {
            MDC.remove("metric");
            tags.keySet().forEach(MDC::remove);
        }
    }
}