package com.syntaxmath.backend.oauth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.service.SessionService;
import com.syntaxmath.backend.auth.service.OAuthMfaChallengeService;
import com.syntaxmath.backend.auth.service.AuthSecurityEventLogger;
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
    private final OAuthMfaChallengeService oAuthMfaChallengeService;
    private final AuthSecurityEventLogger securityEventLogger;

    public OAuth2LoginSuccessHandler(SessionService sessionService,
                                     SessionCookieUtil sessionCookieUtil,
                                     AuthProperties authProperties,
                                     OAuthMfaChallengeService oAuthMfaChallengeService,
                                     AuthSecurityEventLogger securityEventLogger) {
        this.sessionService = sessionService;
        this.sessionCookieUtil = sessionCookieUtil;
        this.authProperties = authProperties;
        this.oAuthMfaChallengeService = oAuthMfaChallengeService;
        this.securityEventLogger = securityEventLogger;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof SyntaxMathOidcUser principal)) {
            throw new IllegalStateException("Unexpected authentication principal: " + authentication.getPrincipal());
        }
        User user = principal.getAppUser();
        if (user.isMfaEnabled()) {
            String challenge = oAuthMfaChallengeService.createChallenge(user.getId());
            securityEventLogger.oauthMfaChallengeStarted(user.getEmail());
            sessionCookieUtil.setOAuthMfaChallengeCookie(response, challenge);
            response.sendRedirect(authProperties.getAllowedOrigin() + "/oauth/mfa");
            return;
        }

        String rawToken = sessionService.createSession(user.getId());
        securityEventLogger.successfulLogin(user.getEmail(), request.getRemoteAddr());
        sessionCookieUtil.setSessionCookie(response, rawToken);

        response.sendRedirect(authProperties.getAllowedOrigin());
    }
}
