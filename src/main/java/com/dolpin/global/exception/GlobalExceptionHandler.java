package com.dolpin.global.exception;

import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Method argument type mismatch: parameter '{}' with value '{}' could not be converted to type '{}'",
                e.getName(), e.getValue(), e.getRequiredType().getSimpleName());

        String message = String.format("잘못된 파라미터입니다: %s", e.getName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(message)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("유효하지 않은 입력입니다");

        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(message)));
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
}
