package com.syntaxmath.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "syntaxmath.auth")
public class AuthProperties {

    private String cookieName = "syntaxmath_session";
    private boolean cookieSecure = true;
    private Duration sessionLifetime = Duration.ofDays(7);
    private String allowedOrigin;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public Duration getSessionLifetime() {
        return sessionLifetime;
    }

    public void setSessionLifetime(Duration sessionLifetime) {
        this.sessionLifetime = sessionLifetime;
    }

    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    public void setAllowedOrigin(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }
}
