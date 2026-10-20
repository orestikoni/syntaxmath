package com.syntaxmath.backend.auth.util;

import com.syntaxmath.backend.auth.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieUtil {

    private final AuthProperties authProperties;

    public SessionCookieUtil(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void setSessionCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(authProperties.getCookieName(), rawToken)
                .httpOnly(true)
                .secure(authProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(authProperties.getSessionLifetime())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(authProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(authProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authProperties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
