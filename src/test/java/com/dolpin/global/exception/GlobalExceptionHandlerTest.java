package com.dolpin.global.exception;

import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystemException;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Nested
    @DisplayName("BusinessException 처리 테스트")
    class BusinessExceptionTest {

        @Test
        @DisplayName("4xx 클라이언트 에러 처리")
        void handleBusinessException_4xxError() {
            // given
            ResponseStatus responseStatus = ResponseStatus.INVALID_PARAMETER.withMessage("잘못된 파라미터");
            BusinessException exception = new BusinessException(responseStatus);

            // when
            ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("잘못된 파라미터");
        }

        @Test
        @DisplayName("5xx 서버 에러 처리")
        void handleBusinessException_5xxError() {
            // given
            ResponseStatus responseStatus = ResponseStatus.INTERNAL_SERVER_ERROR.withMessage("서버 내부 오류");
            BusinessException exception = new BusinessException(responseStatus);

            // when
            ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류");
        }

        @Test
        @DisplayName("UNAUTHORIZED 에러 처리")
        void handleBusinessException_UnauthorizedError() {
            // given
            ResponseStatus responseStatus = ResponseStatus.UNAUTHORIZED.withMessage("인증이 필요합니다");
            BusinessException exception = new BusinessException(responseStatus);

            // when
            ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("인증이 필요합니다");
        }
    }

    @Nested
    @DisplayName("Validation 예외 처리 테스트")
    class ValidationExceptionTest {

        @Test
        @DisplayName("MethodArgumentNotValidException - 단일 필드 에러")
        void handleMethodArgumentNotValidException_SingleFieldError() {
            // given
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
            bindingResult.addError(new FieldError("testObject", "nickname", "닉네임은 필수입니다"));

            MethodParameter parameter = createMockMethodParameter();
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleMethodArgumentNotValidException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("닉네임은 필수입니다");
        }

        @Test
        @DisplayName("MethodArgumentNotValidException - 다중 필드 에러")
        void handleMethodArgumentNotValidException_MultipleFieldErrors() {
            // given
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
            bindingResult.addError(new FieldError("testObject", "nickname", "닉네임은 필수입니다"));
            bindingResult.addError(new FieldError("testObject", "email", "이메일 형식이 올바르지 않습니다"));

            MethodParameter parameter = createMockMethodParameter();
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleMethodArgumentNotValidException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("닉네임은 필수입니다");
            assertThat(response.getBody().getMessage()).contains("이메일 형식이 올바르지 않습니다");
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException 처리")
        void handleMethodArgumentTypeMismatchException() {
            // given
            MethodParameter parameter = createMockMethodParameter();
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "invalidValue", Long.class, "userId", parameter, new NumberFormatException());

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Parameter 'userId' should be of type Long");
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException - requiredType null인 경우")
        void handleMethodArgumentTypeMismatchException_NullRequiredType() {
            // given
            MethodParameter parameter = createMockMethodParameter();
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "invalidValue", null, "userId", parameter, new NumberFormatException());

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Parameter 'userId' should be of type Unknown");
        }
    }

    @Nested
    @DisplayName("기본 예외 처리 테스트")
    class BasicExceptionTest {

        @Test
        @DisplayName("IllegalArgumentException 처리")
        void handleIllegalArgumentException() {
            // given
            IllegalArgumentException exception = new IllegalArgumentException("잘못된 인수입니다");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleIllegalArgumentException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("잘못된 인수입니다");
        }
    }

    @Nested
    @DisplayName("데이터베이스 관련 예외 처리 테스트")
    class DatabaseExceptionTest {

        @Test
        @DisplayName("InvalidDataAccessResourceUsageException - 파라미터 타입 오류")
        void handleDataAccessException_ParameterTypeError() {
            // given
            String errorMessage = "could not determine data type of parameter $1";
            InvalidDataAccessResourceUsageException exception =
                    new InvalidDataAccessResourceUsageException(errorMessage, new RuntimeException());

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleDataAccessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("데이터베이스 쿼리 파라미터 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("InvalidDataAccessResourceUsageException - 일반 데이터베이스 오류")
        void handleDataAccessException_GeneralError() {
            // given
            String errorMessage = "일반적인 데이터베이스 접근 오류";
            InvalidDataAccessResourceUsageException exception =
                    new InvalidDataAccessResourceUsageException(errorMessage, new RuntimeException());

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleDataAccessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("데이터베이스 접근 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("SQLGrammarException 처리")
        void handleSQLGrammarException() {
            // given
            SQLException sqlException = new SQLException("Table doesn't exist");
            SQLGrammarException exception = new SQLGrammarException("SQL 문법 오류", sqlException, "SELECT * FROM invalid_table");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleSQLGrammarException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("SQL 쿼리 문법 오류가 발생했습니다.");
        }
    }

    @Nested
    @DisplayName("일반 Exception 처리 테스트")
    class GeneralExceptionTest {

        @Test
        @DisplayName("PostgreSQL 관련 예외")
        void handleException_PostgreSQLError() {
            // given
            org.postgresql.util.PSQLException psqlException = mock(org.postgresql.util.PSQLException.class);
            RuntimeException exception = new RuntimeException("DB 연결 실패", psqlException);

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("데이터베이스 연결 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("AI 서비스 관련 예외 - PlaceAiClient 클래스")
        void handleException_AiServiceError_PlaceAiClient() {
            // given
            Exception exception = createExceptionWithStackTrace("com.dolpin.domain.place.client.PlaceAiClient");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("AI 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("AI 서비스 관련 예외 - ai 패키지")
        void handleException_AiServiceError_AiPackage() {
            // given
            Exception exception = createExceptionWithStackTrace("com.dolpin.domain.service.ai.AiService");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("AI 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("AI 서비스 관련 예외 - BusinessException 메시지")
        void handleException_AiServiceError_BusinessExceptionMessage() {
            // given
            BusinessException exception = new BusinessException(
                    ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 호출 실패");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("AI 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("AI 서비스 관련 예외 - 추천 서비스 메시지")
        void handleException_AiServiceError_RecommendMessage() {
            // given
            BusinessException exception = new BusinessException(
                    ResponseStatus.INTERNAL_SERVER_ERROR, "추천 서비스 연결 실패");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("AI 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("외부 API 관련 예외 - RestClientException")
        void handleException_ExternalApiError_RestClient() {
            // given
            RestClientException exception = new RestClientException("외부 API 호출 실패");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("외부 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("외부 API 관련 예외 - HttpClientErrorException")
        void handleException_ExternalApiError_HttpClientError() {
            // given
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("외부 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("외부 API 관련 예외 - ConnectException")
        void handleException_ExternalApiError_ConnectException() {
            // given
            ConnectException connectException = new ConnectException("연결 실패");
            RuntimeException exception = new RuntimeException("외부 호출 실패", connectException);

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("외부 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("외부 API 관련 예외 - SocketTimeoutException")
        void handleException_ExternalApiError_SocketTimeout() {
            // given
            SocketTimeoutException timeoutException = new SocketTimeoutException("타임아웃");
            RuntimeException exception = new RuntimeException("외부 호출 실패", timeoutException);

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("외부 서비스 연동 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("파일 처리 관련 예외 - StorageService 클래스")
        void handleException_FileProcessingError_StorageService() {
            // given
            Exception exception = createExceptionWithStackTrace("com.dolpin.global.storage.service.StorageService");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("파일 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("파일 처리 관련 예외 - IOException")
        void handleException_FileProcessingError_IOException() {
            // given
            IOException exception = new IOException("파일 읽기 실패");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("파일 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("파일 처리 관련 예외 - FileSystemException")
        void handleException_FileProcessingError_FileSystemException() {
            // given
            FileSystemException exception = new FileSystemException("파일 시스템 오류");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("파일 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("파일 처리 관련 예외 - 메시지로 판단")
        void handleException_FileProcessingError_ByMessage() {
            // given
            RuntimeException exception = new RuntimeException("파일 업로드 중 오류 발생");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("파일 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("인증 관련 예외 - AuthService 클래스")
        void handleException_AuthenticationError_AuthService() {
            // given
            Exception exception = createExceptionWithStackTrace("com.dolpin.domain.auth.service.AuthService");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("인증 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("인증 관련 예외 - AuthenticationException")
        void handleException_AuthenticationError_AuthenticationException() {
            // given
            AuthenticationException exception = new BadCredentialsException("인증 실패");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("인증 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("인증 관련 예외 - AccessDeniedException")
        void handleException_AuthenticationError_AccessDenied() {
            // given
            AccessDeniedException exception = new AccessDeniedException("접근 거부");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("인증 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("인증 관련 예외 - 토큰 메시지")
        void handleException_AuthenticationError_TokenMessage() {
            // given
            RuntimeException exception = new RuntimeException("토큰이 만료되었습니다");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("인증 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("인증 관련 예외 - OAuth 메시지")
        void handleException_AuthenticationError_OAuthMessage() {
            // given
            RuntimeException exception = new RuntimeException("OAuth 인증 실패");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("인증 처리 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("일반 예외 처리")
        void handleException_GeneralError() {
            // given
            RuntimeException exception = new RuntimeException("알 수 없는 오류");

            // when
            ResponseEntity<ApiResponse<Object>> response =
                    globalExceptionHandler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");
        }
    }

    // 테스트 헬퍼 메서드
    private Exception createExceptionWithStackTrace(String className) {
        Exception exception = new RuntimeException("테스트 예외");
        StackTraceElement[] stackTrace = {
                new StackTraceElement(className, "testMethod", "TestFile.java", 1)
        };
        exception.setStackTrace(stackTrace);
        return exception;
    }

    private MethodParameter createMockMethodParameter() {
        try {
            // 실제 메서드를 사용해서 MethodParameter 생성
            java.lang.reflect.Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Test method not found", e);
        }
    }

    // 테스트용 더미 컨트롤러 클래스
    private static class TestController {
        public void testMethod(String param) {
            // 테스트용 메서드
        }
    }
}
