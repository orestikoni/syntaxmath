package com.syntaxmath.backend.oauth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.service.SessionService;
import com.syntaxmath.backend.auth.util.SessionCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;


import java.io.IOException;

@Component
public class
OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SessionService sessionService;
    private final SessionCookieUtil sessionCookieUtil;
    private final AuthProperties authProperties;

    public OAuth2LoginSuccessHandler(SessionService sessionService,
                                     SessionCookieUtil sessionCookieUtil,
                                     AuthProperties authProperties) {
        this.sessionService = sessionService;
        this.sessionCookieUtil = sessionCookieUtil;
        this.authProperties = authProperties;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof SyntaxMathOidcUser principal)) {
            throw new IllegalStateException("Unexpected authentication principal: " + authentication.getPrincipal());
        }
        User user = principal.getAppUser();

        String rawToken = sessionService.createSession(user.getId());
        sessionCookieUtil.setSessionCookie(response, rawToken);

        response.sendRedirect(authProperties.getAllowedOrigin());
    }
}
