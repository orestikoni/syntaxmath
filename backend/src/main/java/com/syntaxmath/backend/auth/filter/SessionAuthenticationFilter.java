package com.syntaxmath.backend.auth.filter;

import com.syntaxmath.backend.auth.service.SessionService;
import com.syntaxmath.backend.auth.util.SessionCookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final SessionCookieUtil sessionCookieUtil;

    public SessionAuthenticationFilter(SessionService sessionService, SessionCookieUtil sessionCookieUtil) {
        this.sessionService = sessionService;
        this.sessionCookieUtil = sessionCookieUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = sessionCookieUtil.extractToken(request);
        if (token != null) {
            sessionService.validateSession(token).ifPresent(user -> {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
