package com.dolpin.domain.auth.service.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CookieService {

    @Value("${cookie.domain:}")
    private String cookieDomain;

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken, long expiresIn) {
        log.info("=== ACCESS TOKEN COOKIE DEBUG ===");
        log.info("cookieDomain from properties: '{}'", cookieDomain);
        log.info("cookieDomain isEmpty: {}", cookieDomain == null || cookieDomain.isEmpty());

        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) expiresIn);
        cookie.setAttribute("SameSite", "None");

        // 도메인 설정 로직 디버깅
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            log.info("Setting cookie domain to: '{}'", cookieDomain);
            cookie.setDomain(cookieDomain);
        } else {
            log.info("Cookie domain not set - will use request domain");
        }

        response.addCookie(cookie);
        log.info("Access token cookie added successfully");
        log.info("=====================================");
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        log.info("=== REFRESH TOKEN COOKIE DEBUG ===");
        log.info("cookieDomain from properties: '{}'", cookieDomain);
        log.info("cookieDomain isEmpty: {}", cookieDomain == null || cookieDomain.isEmpty());

        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "None");

        // 도메인 설정 로직 디버깅
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            log.info("Setting cookie domain to: '{}'", cookieDomain);
            cookie.setDomain(cookieDomain);
        } else {
            log.info("Cookie domain not set - will use request domain");
        }

        response.addCookie(cookie);
        log.info("Refresh token cookie added successfully");
        log.info("====================================");
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        log.info("=== DELETE ACCESS TOKEN COOKIE ===");
        log.info("cookieDomain from properties: '{}'", cookieDomain);

        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            log.info("Setting delete cookie domain to: '{}'", cookieDomain);
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);
        log.info("Access token cookie deletion added");
        log.info("==================================");
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        log.info("=== DELETE REFRESH TOKEN COOKIE ===");
        log.info("cookieDomain from properties: '{}'", cookieDomain);

        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            log.info("Setting delete cookie domain to: '{}'", cookieDomain);
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);
        log.info("Refresh token cookie deletion added");
        log.info("===================================");
    }
}