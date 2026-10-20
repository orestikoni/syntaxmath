package com.syntaxmath.backend.auth.filter;

import com.syntaxmath.backend.auth.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> CHECKED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String OAUTH_CALLBACK_PATH = "/login/oauth2/code";

    private final AuthProperties authProperties;

    public OriginCheckFilter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (CHECKED_METHODS.contains(request.getMethod()) && !isOAuthCallback(request)) {
            String origin = request.getHeader("Origin");
            String allowedOrigin = authProperties.getAllowedOrigin();
            if (origin == null || origin.isBlank()
                    || allowedOrigin == null || allowedOrigin.isBlank()
                    || !origin.equals(allowedOrigin)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin not allowed");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOAuthCallback(HttpServletRequest request) {
        String requestUri = request.getServletPath();
        return requestUri.equals(OAUTH_CALLBACK_PATH)
                || requestUri.startsWith(OAUTH_CALLBACK_PATH + "/");
    }
}
