package com.syntaxmath.backend.auth.controller;

import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.dto.EmailRequest;
import com.syntaxmath.backend.auth.dto.LoginRequest;
import com.syntaxmath.backend.auth.dto.MeResponse;
import com.syntaxmath.backend.auth.dto.MfaCodeRequest;
import com.syntaxmath.backend.auth.dto.MfaEnrollmentResponse;
import com.syntaxmath.backend.auth.dto.MfaRecoveryCodesResponse;
import com.syntaxmath.backend.auth.dto.PasswordResetRequest;
import com.syntaxmath.backend.auth.dto.RegisterRequest;
import com.syntaxmath.backend.auth.dto.SessionResponse;
import com.syntaxmath.backend.auth.dto.TokenRequest;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import com.syntaxmath.backend.auth.service.AuthActionService;
import com.syntaxmath.backend.auth.service.AuthSecurityEventLogger;
import com.syntaxmath.backend.auth.service.LoginRateLimitService;
import com.syntaxmath.backend.auth.service.OAuthMfaChallengeService;
import com.syntaxmath.backend.auth.service.SessionService;
import com.syntaxmath.backend.auth.service.TotpMfaService;
import com.syntaxmath.backend.auth.util.SessionCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthActionService authActionService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final LoginRateLimitService loginRateLimitService;
    private final AuthSecurityEventLogger securityEventLogger;
    private final TotpMfaService totpMfaService;
    private final OAuthMfaChallengeService oAuthMfaChallengeService;
    private final SessionCookieUtil sessionCookieUtil;
    private final String dummyPasswordHash;

    public AuthController(UserRepository userRepository,
                           AuthActionService authActionService,
                           PasswordEncoder passwordEncoder,
                           SessionService sessionService,
                           LoginRateLimitService loginRateLimitService,
                           AuthSecurityEventLogger securityEventLogger,
                           TotpMfaService totpMfaService,
                           OAuthMfaChallengeService oAuthMfaChallengeService,
                           SessionCookieUtil sessionCookieUtil) {
        this.userRepository = userRepository;
        this.authActionService = authActionService;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.loginRateLimitService = loginRateLimitService;
        this.securityEventLogger = securityEventLogger;
        this.totpMfaService = totpMfaService;
        this.oAuthMfaChallengeService = oAuthMfaChallengeService;
        this.sessionCookieUtil = sessionCookieUtil;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request,
                                                        HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Duration retryAfter = loginRateLimitService.checkAndRecordAttempt(
                httpRequest.getRemoteAddr(),
                email
        );
        if (!retryAfter.isZero()) {
            return tooManyAttempts(retryAfter);
        }
        authActionService.register(email, request.password(), request.firstName(), request.lastName());
        return accepted("If the address can be registered, a verification email will be sent.");
    }

    @PostMapping("/verification/request")
    public ResponseEntity<Map<String, String>> requestEmailVerification(
            @Valid @RequestBody EmailRequest request,
            HttpServletRequest httpRequest) {
        Duration retryAfter = loginRateLimitService.checkAndRecordAttempt(
                httpRequest.getRemoteAddr(),
                request.email()
        );
        if (!retryAfter.isZero()) {
            return tooManyAttempts(retryAfter);
        }
        authActionService.requestEmailVerification(request.email());
        return accepted("If the address requires verification, an email will be sent.");
    }

    @PostMapping("/verification/confirm")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody TokenRequest request) {
        authActionService.verifyEmail(request.token());
        return ResponseEntity.ok(Map.of("message", "Email verified. You can now sign in."));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody EmailRequest request,
            HttpServletRequest httpRequest) {
        Duration retryAfter = loginRateLimitService.checkAndRecordAttempt(
                httpRequest.getRemoteAddr(),
                request.email()
        );
        if (!retryAfter.isZero()) {
            return tooManyAttempts(retryAfter);
        }
        authActionService.requestPasswordReset(request.email());
        return accepted("If the account is eligible, a password reset email will be sent.");
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        authActionService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(Map.of("message", "Password updated. Sign in with your new password."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        var retryAfter = loginRateLimitService.checkAndRecordAttempt(
                httpRequest.getRemoteAddr(),
                email
        );
        if (!retryAfter.isZero()) {
            return tooManyAttempts(retryAfter);
        }

        Duration accountLockout = loginRateLimitService.getAccountLockout(email);
        if (!accountLockout.isZero()) {
            securityEventLogger.accountLocked(email, httpRequest.getRemoteAddr());
            return tooManyAttempts(accountLockout);
        }

        Optional<User> maybeUser = userRepository.findByEmail(email);

        String passwordHash = maybeUser
                .map(User::getPasswordHash)
                .orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        boolean invalid = maybeUser.isEmpty()
                || maybeUser.get().getPasswordHash() == null
                || !passwordMatches;

        if (invalid) {
            Duration lockout = loginRateLimitService.recordFailedLogin(email);
            securityEventLogger.failedLogin(email, httpRequest.getRemoteAddr());
            if (!lockout.isZero()) {
                securityEventLogger.accountLocked(email, httpRequest.getRemoteAddr());
                return tooManyAttempts(lockout);
            }
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }

        User user = maybeUser.get();
        if (!user.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
        if (user.isMfaEnabled()) {
            if (request.mfaCode() == null || request.mfaCode().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.<String, Object>of(
                                "message", "Enter your authenticator or recovery code.",
                                "mfaRequired", true
                        ));
            }
            if (!totpMfaService.verifyLoginCode(user, request.mfaCode())) {
                Duration lockout = loginRateLimitService.recordFailedLogin(email);
                securityEventLogger.failedLogin(email, httpRequest.getRemoteAddr());
                if (!lockout.isZero()) {
                    securityEventLogger.accountLocked(email, httpRequest.getRemoteAddr());
                    return tooManyAttempts(lockout);
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid email, password, or authenticator code."));
            }
        }
        loginRateLimitService.clearFailedLogins(email);
        String rawToken = sessionService.createSession(user.getId());
        securityEventLogger.successfulLogin(email, httpRequest.getRemoteAddr());
        sessionCookieUtil.setSessionCookie(response, rawToken);

        return ResponseEntity.ok(toMeResponse(user));
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<Map<String, Boolean>> mfaStatus(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("enabled", user.isMfaEnabled()));
    }

    @PostMapping("/mfa/enroll")
    public ResponseEntity<MfaEnrollmentResponse> beginMfaEnrollment(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(totpMfaService.beginEnrollment(user));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<MfaRecoveryCodesResponse> enableMfa(
            @Valid @RequestBody MfaCodeRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(totpMfaService.enable(user, request.code()));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(
            @Valid @RequestBody MfaCodeRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        totpMfaService.disable(user, request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/oauth/complete")
    public ResponseEntity<?> completeOAuthMfa(
            @Valid @RequestBody MfaCodeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String challengeToken = sessionCookieUtil.extractOAuthMfaChallenge(httpRequest);
        Duration retryAfter = loginRateLimitService.checkAndRecordAttempt(
                httpRequest.getRemoteAddr(),
                challengeToken == null ? "missing-oauth-challenge" : challengeToken
        );
        if (!retryAfter.isZero()) {
            return tooManyAttempts(retryAfter);
        }
        Optional<User> challengeUser = oAuthMfaChallengeService.findChallengeUser(challengeToken);
        if (challengeUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired sign-in challenge"));
        }

        User user = challengeUser.get();
        Duration lockout = loginRateLimitService.getAccountLockout(user.getEmail());
        if (!lockout.isZero()) {
            return tooManyAttempts(lockout);
        }
        try {
            User authenticatedUser = oAuthMfaChallengeService.completeChallenge(challengeToken, request.code());
            loginRateLimitService.clearFailedLogins(authenticatedUser.getEmail());
            String rawToken = sessionService.createSession(authenticatedUser.getId());
            securityEventLogger.successfulLogin(authenticatedUser.getEmail(), httpRequest.getRemoteAddr());
            sessionCookieUtil.setSessionCookie(response, rawToken);
            sessionCookieUtil.clearOAuthMfaChallengeCookie(response);
            return ResponseEntity.ok(toMeResponse(authenticatedUser));
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
                throw exception;
            }
            Duration failedLoginLockout = loginRateLimitService.recordFailedLogin(user.getEmail());
            securityEventLogger.failedLogin(user.getEmail(), httpRequest.getRemoteAddr());
            if (!failedLoginLockout.isZero()) {
                securityEventLogger.accountLocked(user.getEmail(), httpRequest.getRemoteAddr());
                return tooManyAttempts(failedLoginLockout);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired sign-in challenge"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = sessionCookieUtil.extractToken(request);
        sessionService.revokeSession(token);
        sessionCookieUtil.clearSessionCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(
            Authentication authentication,
            HttpServletRequest request) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(sessionService.listActiveSessions(
                user.getId(),
                sessionCookieUtil.extractToken(request)
        ));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable UUID sessionId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = (User) authentication.getPrincipal();
        UUID currentSessionId = sessionService.findSessionIdForToken(
                user.getId(),
                sessionCookieUtil.extractToken(request)
        ).orElse(null);

        if (sessionService.revokeSession(user.getId(), sessionId)) {
            securityEventLogger.sessionsRevoked(user.getEmail(), 1, "single");
        }
        if (sessionId.equals(currentSessionId)) {
            sessionCookieUtil.clearSessionCookie(response);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            Authentication authentication,
            HttpServletResponse response) {
        User user = (User) authentication.getPrincipal();
        int revokedCount = sessionService.revokeAllSessions(user.getId());
        sessionCookieUtil.clearSessionCookie(response);
        securityEventLogger.sessionsRevoked(user.getEmail(), revokedCount, "all");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(toMeResponse(user));
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }

    private ResponseEntity<Map<String, String>> tooManyAttempts(Duration retryAfter) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter.toSeconds()))
                .body(Map.of("message", "Too many login attempts. Try again later."));
    }

    private ResponseEntity<Map<String, String>> accepted(String message) {
        return ResponseEntity.accepted().body(Map.of("message", message));
    }
}
