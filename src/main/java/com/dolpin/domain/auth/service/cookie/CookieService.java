package com.dolpin.domain.auth.service.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieService {

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken, long expiresIn) {
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTP를 위해 false 유지
        cookie.setPath("/");
        cookie.setMaxAge((int) expiresIn);
        cookie.setAttribute("SameSite", "Lax"); // None에서 Lax로 변경
        response.addCookie(cookie);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTP를 위해 false 유지
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Lax"); // None에서 Lax로 변경
        response.addCookie(cookie);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
