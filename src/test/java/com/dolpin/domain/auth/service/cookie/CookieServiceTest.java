package com.dolpin.domain.auth.service.cookie;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dolpin.global.constants.AuthTestConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieService 테스트")
class CookieServiceTest {

    @Mock
    private HttpServletResponse response;

    private CookieService cookieService;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    private ArgumentCaptor<Cookie> cookieCaptor;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        // Logger 설정
        logger = (Logger) LoggerFactory.getLogger(CookieService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Nested
    @DisplayName("Access Token Cookie 추가 테스트")
    class AddAccessTokenCookieTest {

        @Test
        @DisplayName("쿠키 도메인이 설정된 경우")
        void addAccessTokenCookie_WithDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", AuthTestConstants.TEST_COOKIE_DOMAIN);
            String accessToken = AuthTestConstants.JWT_ACCESS_TOKEN;
            long expiresIn = AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN;

            // when
            cookieService.addAccessTokenCookie(response, accessToken, expiresIn);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("access_token");
            assertThat(capturedCookie.getValue()).isEqualTo(accessToken);
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo((int) expiresIn);
            assertThat(capturedCookie.getDomain()).isEqualTo(AuthTestConstants.TEST_COOKIE_DOMAIN);

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            assertThat(logEvents).hasSizeGreaterThan(0);
            assertThat(logEvents.get(0).getFormattedMessage()).contains("ACCESS TOKEN COOKIE DEBUG");

            boolean domainLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Setting cookie domain to"));
            assertThat(domainLogFound).isTrue();
        }

        @Test
        @DisplayName("쿠키 도메인이 null인 경우")
        void addAccessTokenCookie_WithNullDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", null);
            String accessToken = AuthTestConstants.JWT_ACCESS_TOKEN;
            long expiresIn = AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN;

            // when
            cookieService.addAccessTokenCookie(response, accessToken, expiresIn);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("access_token");
            assertThat(capturedCookie.getValue()).isEqualTo(accessToken);
            assertThat(capturedCookie.getDomain()).isNull();

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean noDomainLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Cookie domain not set"));
            assertThat(noDomainLogFound).isTrue();
        }

        @Test
        @DisplayName("쿠키 도메인이 빈 문자열인 경우")
        void addAccessTokenCookie_WithEmptyDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", "");
            String accessToken = AuthTestConstants.JWT_ACCESS_TOKEN;
            long expiresIn = AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN;

            // when
            cookieService.addAccessTokenCookie(response, accessToken, expiresIn);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getDomain()).isNull();

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean emptyDomainLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("cookieDomain isEmpty: true"));
            assertThat(emptyDomainLogFound).isTrue();
        }

        @Test
        @DisplayName("쿠키 속성 설정 검증")
        void addAccessTokenCookie_CookieAttributes() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", AuthTestConstants.TEST_COOKIE_DOMAIN);
            String accessToken = AuthTestConstants.JWT_ACCESS_TOKEN;
            long expiresIn = AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN;

            // when
            cookieService.addAccessTokenCookie(response, accessToken, expiresIn);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();

            // SameSite 속성은 getAttribute로 확인할 수 없으므로 로직 실행만 확인
            assertThat(capturedCookie.getName()).isEqualTo("access_token");
            assertThat(capturedCookie.getValue()).isEqualTo(accessToken);
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo((int) expiresIn);
        }
    }

    @Nested
    @DisplayName("Refresh Token Cookie 추가 테스트")
    class AddRefreshTokenCookieTest {

        @Test
        @DisplayName("리프레시 토큰 쿠키 추가 - 도메인 설정")
        void addRefreshTokenCookie_WithDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", AuthTestConstants.TEST_COOKIE_DOMAIN);
            String refreshToken = AuthTestConstants.REFRESH_TOKEN_VALUE;

            // when
            cookieService.addRefreshTokenCookie(response, refreshToken);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("refresh_token");
            assertThat(capturedCookie.getValue()).isEqualTo(refreshToken);
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo(AuthTestConstants.REFRESH_TOKEN_EXPIRES_IN);
            assertThat(capturedCookie.getDomain()).isEqualTo(AuthTestConstants.TEST_COOKIE_DOMAIN);

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean refreshLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("REFRESH TOKEN COOKIE DEBUG"));
            assertThat(refreshLogFound).isTrue();
        }

        @Test
        @DisplayName("리프레시 토큰 쿠키 추가 - 도메인 없음")
        void addRefreshTokenCookie_WithoutDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", null);
            String refreshToken = AuthTestConstants.REFRESH_TOKEN_VALUE;

            // when
            cookieService.addRefreshTokenCookie(response, refreshToken);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("refresh_token");
            assertThat(capturedCookie.getValue()).isEqualTo(refreshToken);
            assertThat(capturedCookie.getDomain()).isNull();

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean successLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Refresh token cookie added successfully"));
            assertThat(successLogFound).isTrue();
        }
    }

    @Nested
    @DisplayName("Access Token Cookie 삭제 테스트")
    class DeleteAccessTokenCookieTest {

        @Test
        @DisplayName("액세스 토큰 쿠키 삭제 - 도메인 설정")
        void deleteAccessTokenCookie_WithDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", AuthTestConstants.TEST_COOKIE_DOMAIN);

            // when
            cookieService.deleteAccessTokenCookie(response);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("access_token");
            assertThat(capturedCookie.getValue()).isNull();
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo(0);
            assertThat(capturedCookie.getDomain()).isEqualTo(AuthTestConstants.TEST_COOKIE_DOMAIN);

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean deleteLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("DELETE ACCESS TOKEN COOKIE"));
            assertThat(deleteLogFound).isTrue();
        }

        @Test
        @DisplayName("액세스 토큰 쿠키 삭제 - 도메인 없음")
        void deleteAccessTokenCookie_WithoutDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", "");

            // when
            cookieService.deleteAccessTokenCookie(response);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("access_token");
            assertThat(capturedCookie.getValue()).isNull();
            assertThat(capturedCookie.getMaxAge()).isEqualTo(0);
            assertThat(capturedCookie.getDomain()).isNull();

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean deletionLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Access token cookie deletion added"));
            assertThat(deletionLogFound).isTrue();
        }
    }

    @Nested
    @DisplayName("Refresh Token Cookie 삭제 테스트")
    class DeleteRefreshTokenCookieTest {

        @Test
        @DisplayName("리프레시 토큰 쿠키 삭제 - 도메인 설정")
        void deleteRefreshTokenCookie_WithDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", AuthTestConstants.TEST_COOKIE_DOMAIN);

            // when
            cookieService.deleteRefreshTokenCookie(response);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo("refresh_token");
            assertThat(capturedCookie.getValue()).isNull();
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo(0);
            assertThat(capturedCookie.getDomain()).isEqualTo(AuthTestConstants.TEST_COOKIE_DOMAIN);

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean deleteRefreshLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("DELETE REFRESH TOKEN COOKIE"));
            assertThat(deleteRefreshLogFound).isTrue();
        }

        @Test
        @DisplayName("리프레시 토큰 쿠키 삭제 - 도메인 없음")
        void deleteRefreshTokenCookie_WithoutDomain() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", null);

            // when
            cookieService.deleteRefreshTokenCookie(response);

            // then
            then(response).should().addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getDomain()).isNull();

            // 로그 검증
            List<ILoggingEvent> logEvents = listAppender.list;
            boolean successLogFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Refresh token cookie deletion added"));
            assertThat(successLogFound).isTrue();
        }
    }

    @Nested
    @DisplayName("로깅 검증 테스트")
    class LoggingVerificationTest {

        @Test
        @DisplayName("모든 메서드가 적절한 로그를 출력하는지 검증")
        void verifyAllMethodsProduceAppropriateLogging() {
            // given
            ReflectionTestUtils.setField(cookieService, "cookieDomain", AuthTestConstants.TEST_COOKIE_DOMAIN);

            // when - 모든 메서드 호출
            cookieService.addAccessTokenCookie(response, AuthTestConstants.JWT_ACCESS_TOKEN, AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN);
            cookieService.addRefreshTokenCookie(response, AuthTestConstants.REFRESH_TOKEN_VALUE);
            cookieService.deleteAccessTokenCookie(response);
            cookieService.deleteRefreshTokenCookie(response);

            // then
            then(response).should(times(4)).addCookie(cookieCaptor.capture());

            List<ILoggingEvent> logEvents = listAppender.list;
            assertThat(logEvents).hasSizeGreaterThan(0);

            // 각 메서드의 로그가 존재하는지 확인
            boolean accessTokenDebugFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("ACCESS TOKEN COOKIE DEBUG"));
            boolean refreshTokenDebugFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("REFRESH TOKEN COOKIE DEBUG"));
            boolean deleteAccessFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("DELETE ACCESS TOKEN COOKIE"));
            boolean deleteRefreshFound = logEvents.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("DELETE REFRESH TOKEN COOKIE"));

            assertThat(accessTokenDebugFound).isTrue();
            assertThat(refreshTokenDebugFound).isTrue();
            assertThat(deleteAccessFound).isTrue();
            assertThat(deleteRefreshFound).isTrue();
        }
    }
}
