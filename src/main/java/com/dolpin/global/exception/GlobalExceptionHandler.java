package com.dolpin.global.exception;

import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        HttpStatus status = e.getResponseStatus().getHttpStatus();

        if (status.is5xxServerError()) {
            log.error("Business exception occurred: {}", e.getMessage(), e);
        } else if (status.is4xxClientError()) {
            log.warn("Business exception occurred: {}", e.getMessage());
        }

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(e.getResponseStatus()));
    }

    // Validation 예외 처리 추가
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation exception occurred: {}", e.getMessage());

        // 첫 번째 필드 에러 메시지를 사용하거나, 모든 에러를 조합
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(errorMessage)));
    }

    // 타입 변환 예외 처리 추가
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch exception occurred: {}", e.getMessage());

        String parameterName = e.getName();
        String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "Unknown";
        String errorMessage = String.format("Parameter '%s' should be of type %s", parameterName, expectedType);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(errorMessage)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument exception occurred: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(e.getMessage())));
    }

    // PostgreSQL 관련 예외 처리
    @ExceptionHandler(org.springframework.dao.InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            org.springframework.dao.InvalidDataAccessResourceUsageException e) {
        log.error("Database access error occurred: {}", e.getMessage(), e);

        // PostgreSQL 파라미터 타입 오류 체크
        if (e.getMessage().contains("could not determine data type of parameter")) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR
                            .withMessage("데이터베이스 쿼리 파라미터 오류가 발생했습니다.")));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR
                        .withMessage("데이터베이스 접근 중 오류가 발생했습니다.")));
    }

    // JPA/Hibernate 예외 처리
    @ExceptionHandler(org.hibernate.exception.SQLGrammarException.class)
    public ResponseEntity<ApiResponse<Object>> handleSQLGrammarException(
            org.hibernate.exception.SQLGrammarException e) {
        log.error("SQL grammar error occurred: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR
                        .withMessage("SQL 쿼리 문법 오류가 발생했습니다.")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unhandled exception occurred: ", e);

        // 구체적인 에러 타입별로 다른 메시지 제공
        String errorMessage = "서버 내부 오류가 발생했습니다.";

        // PostgreSQL 관련 에러
        if (e.getCause() instanceof org.postgresql.util.PSQLException) {
            errorMessage = "데이터베이스 연결 오류가 발생했습니다.";
        }
        // AI 서비스 관련 에러는 더 구체적으로 체크
        else if (isAiServiceError(e)) {
            errorMessage = "AI 서비스 연동 중 오류가 발생했습니다.";
        }
        // RestTemplate 관련 에러 (외부 API 호출 에러)
        else if (isExternalApiError(e)) {
            errorMessage = "외부 서비스 연동 중 오류가 발생했습니다.";
        }
        // 파일 처리 관련 에러
        else if (isFileProcessingError(e)) {
            errorMessage = "파일 처리 중 오류가 발생했습니다.";
        }
        // 인증/권한 관련 에러
        else if (isAuthenticationError(e)) {
            errorMessage = "인증 처리 중 오류가 발생했습니다.";
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR.withMessage(errorMessage)));
    }

    private boolean isAiServiceError(Exception e) {
        // 스택 트레이스에서 AI 관련 클래스 체크
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // PlaceAiClient 또는 ai 패키지 관련 클래스에서 발생한 예외
            if (className.contains("PlaceAiClient") ||
                    className.contains(".ai.") ||
                    className.contains("AiService")) {
                return true;
            }
        }

        // BusinessException이면서 AI 서비스 관련 메시지인 경우
        if (e instanceof BusinessException) {
            String message = e.getMessage();
            return message != null && (
                    message.contains("AI 서비스") ||
                            message.contains("추천 서비스") ||
                            message.contains("AI API")
            );
        }

        return false;
    }

    private boolean isExternalApiError(Exception e) {
        // RestTemplate 관련 예외들
        return e instanceof org.springframework.web.client.RestClientException ||
                e instanceof org.springframework.web.client.HttpClientErrorException ||
                e instanceof org.springframework.web.client.HttpServerErrorException ||
                e instanceof org.springframework.web.client.ResourceAccessException ||
                e.getCause() instanceof java.net.ConnectException ||
                e.getCause() instanceof java.net.SocketTimeoutException;
    }

    private boolean isFileProcessingError(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("StorageService") ||
                    className.contains("GcsStorage") ||
                    className.contains("FileUpload") ||
                    className.contains("ImageProcess")) {
                return true;
            }
        }

        return e instanceof java.io.IOException ||
                e instanceof java.nio.file.FileSystemException ||
                (e.getMessage() != null && (
                        e.getMessage().contains("파일") ||
                                e.getMessage().contains("업로드") ||
                                e.getMessage().contains("storage")
                ));
    }

    private boolean isAuthenticationError(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("AuthService") ||
                    className.contains("TokenService") ||
                    className.contains("JwtTokenProvider") ||
                    className.contains("OAuthService") ||
                    className.contains("CookieService")) {
                return true;
            }
        }

        return e instanceof org.springframework.security.core.AuthenticationException ||
                e instanceof org.springframework.security.access.AccessDeniedException ||
                (e.getMessage() != null && (
                        e.getMessage().contains("토큰") ||
                                e.getMessage().contains("인증") ||
                                e.getMessage().contains("권한") ||
                                e.getMessage().contains("OAuth")
                ));
    }
}
