package com.dolpin.domain.auth.service.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieService {

    @Value("${cookie.domain:}")
    private String cookieDomain;

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken, long expiresIn) {
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) expiresIn);
        cookie.setAttribute("SameSite", "None");
        cookie.setDomain(cookieDomain);
        response.addCookie(cookie);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "None");
        cookie.setDomain(cookieDomain);
        response.addCookie(cookie);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setDomain(cookieDomain);
        response.addCookie(cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setDomain(cookieDomain);
        response.addCookie(cookie);
    }
}
