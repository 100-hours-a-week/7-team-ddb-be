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
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(false); // JavaScript에서 접근 가능하도록 변경
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) expiresIn);
        
        // 도메인 설정 (빈 문자열이면 설정하지 않음)
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
        
        // SameSite=None을 위한 추가 헤더 설정
        String cookieHeader = String.format("access_token=%s; Max-Age=%d; Path=/; Secure; SameSite=None", 
                                          accessToken, (int) expiresIn);
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookieHeader += "; Domain=" + cookieDomain;
        }
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.info("Access token cookie added with domain: {}", cookieDomain);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true); // 리프레시 토큰은 보안상 HttpOnly 유지
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        
        // 도메인 설정 (빈 문자열이면 설정하지 않음)
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
        
        // SameSite=None을 위한 추가 헤더 설정
        String cookieHeader = String.format("refresh_token=%s; Max-Age=%d; Path=/; Secure; HttpOnly; SameSite=None", 
                                          refreshToken, 14 * 24 * 60 * 60);
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookieHeader += "; Domain=" + cookieDomain;
        }
        response.addHeader("Set-Cookie", cookieHeader);
        
        log.info("Refresh token cookie added with domain: {}", cookieDomain);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(false);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.ad