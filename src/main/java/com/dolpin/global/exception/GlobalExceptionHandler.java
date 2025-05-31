package com.dolpin.global.exception;

import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        // HTTP 상태 코드에 따라 로그 레벨 차별화
        HttpStatus status = e.getResponseStatus().getHttpStatus();

        if (status.is5xxServerError()) {
            // 서버 오류는 ERROR 레벨 유지
            log.error("Business exception occurred: {}", e.getMessage());
        } else if (status.is4xxClientError()) {
            // 클라이언트 오류는 WARN으로 변경 (BadRequest, NotFound 등)
            log.warn("Business exception occurred: {}", e.getMessage());
        } else {
            // 그 외는 INFO로 처리
            log.info("Business exception occurred: {}", e.getMessage());
        }

        return ResponseEntity
                .status(e.getResponseStatus().getHttpStatus())
                .body(ApiResponse.error(e.getResponseStatus()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument exception occurred: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(e.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unhandled exception occurred: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Method argument type mismatch: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(e.getMessage())));
    }
}
