package com.syntaxmath.backend.auth.controller;

import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.dto.EmailRequest;
import com.syntaxmath.backend.auth.dto.LoginRequest;
import com.syntaxmath.backend.auth.dto.MeResponse;
import com.syntaxmath.backend.auth.dto.PasswordResetRequest;
import com.syntaxmath.backend.auth.dto.RegisterRequest;
import com.syntaxmath.backend.auth.dto.TokenRequest;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import com.syntaxmath.backend.auth.service.AuthActionService;
import com.syntaxmath.backend.auth.service.AuthSecurityEventLogger;
import com.syntaxmath.backend.auth.service.LoginRateLimitService;
import com.syntaxmath.backend.auth.service.SessionService;
import com.syntaxmath.backend.auth.util.SessionCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthActionService authActionService;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final LoginRateLimitService loginRateLimitService;
    private final AuthSecurityEventLogger securityEventLogger;
    private final SessionCookieUtil sessionCookieUtil;

    public AuthController(UserRepository userRepository,
                           AuthActionService authActionService,
                           PasswordEncoder passwordEncoder,
                           SessionService sessionService,
                           LoginRateLimitService loginRateLimitService,
                           AuthSecurityEventLogger securityEventLogger,
                           SessionCookieUtil sessionCookieUtil) {
        this.userRepository = userRepository;
        this.authActionService = authActionService;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.loginRateLimitService = loginRateLimitService;
        this.securityEventLogger = securityEventLogger;
        this.sessionCookieUtil = sessionCookieUtil;
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

        boolean invalid = maybeUser.isEmpty()
                || maybeUser.get().getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), maybeUser.get().getPasswordHash());

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Verify your email before signing in."));
        }
        loginRateLimitService.clearFailedLogins(email);
        String rawToken = sessionService.createSession(user.getId());
        sessionCookieUtil.setSessionCookie(response, rawToken);
        securityEventLogger.successfulLogin(email, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(toMeResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = sessionCookieUtil.extractToken(request);
        sessionService.revokeSession(token);
        sessionCookieUtil.clearSessionCookie(response);
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
