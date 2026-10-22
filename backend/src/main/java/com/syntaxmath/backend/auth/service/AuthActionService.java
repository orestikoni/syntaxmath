package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import com.syntaxmath.backend.auth.entity.AuthActionToken;
import com.syntaxmath.backend.auth.entity.AuthActionType;
import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.interfaces.AuthActionTokenRepository;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AuthActionService {

    private final AuthProperties authProperties;
    private final AuthActionTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final CompromisedPasswordService compromisedPasswordService;
    private final ResendEmailService emailService;
    private final AuthSecurityEventLogger securityEventLogger;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthActionService(AuthProperties authProperties,
                             AuthActionTokenRepository tokenRepository,
                             UserRepository userRepository,
                             SessionService sessionService,
                             PasswordEncoder passwordEncoder,
                             CompromisedPasswordService compromisedPasswordService,
                             ResendEmailService emailService,
                             AuthSecurityEventLogger securityEventLogger) {
        this.authProperties = authProperties;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.compromisedPasswordService = compromisedPasswordService;
        this.emailService = emailService;
        this.securityEventLogger = securityEventLogger;
    }

    @Transactional
    public void register(String email, String password, String firstName, String lastName) {
        rejectCompromisedPassword(password);
        String normalizedEmail = normalize(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return;
        }

        User user = userRepository.save(new User(
                normalizedEmail,
                passwordEncoder.encode(password),
                firstName,
                lastName
        ));
        securityEventLogger.registrationCompleted(user.getEmail());
        issueToken(user, AuthActionType.EMAIL_VERIFICATION);
    }

    @Transactional
    public void requestEmailVerification(String email) {
        userRepository.findByEmail(normalize(email))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> issueToken(user, AuthActionType.EMAIL_VERIFICATION));
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        OffsetDateTime now = OffsetDateTime.now();
        AuthActionToken token = findActiveToken(rawToken, AuthActionType.EMAIL_VERIFICATION, now);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid or expired token"));
        token.consume(now);
        user.markEmailVerified();
        tokenRepository.save(token);
        userRepository.save(user);
        securityEventLogger.emailVerified(user.getEmail());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(normalize(email))
                .filter(User::isEmailVerified)
                .ifPresent(user -> issueToken(user, AuthActionType.PASSWORD_RESET));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        OffsetDateTime now = OffsetDateTime.now();
        AuthActionToken token = findActiveToken(rawToken, AuthActionType.PASSWORD_RESET, now);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid or expired token"));
        rejectCompromisedPassword(newPassword);

        token.consume(now);
        user.changePassword(passwordEncoder.encode(newPassword));
        tokenRepository.save(token);
        userRepository.save(user);
        sessionService.revokeAllSessions(user.getId());
        securityEventLogger.passwordReset(user.getEmail());
    }

    private void issueToken(User user, AuthActionType purpose) {
        OffsetDateTime now = OffsetDateTime.now();
        tokenRepository.findAllByUserIdAndPurposeAndConsumedAtIsNull(user.getId(), purpose)
                .forEach(token -> token.consume(now));
        tokenRepository.flush();

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Duration lifetime = purpose == AuthActionType.EMAIL_VERIFICATION
                ? authProperties.getEmailVerificationLifetime()
                : authProperties.getPasswordResetLifetime();
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalStateException("Authentication token lifetimes must be positive");
        }

        tokenRepository.save(new AuthActionToken(
                user.getId(),
                hash(rawToken),
                purpose,
                now.plus(lifetime)
        ));

        if (purpose == AuthActionType.EMAIL_VERIFICATION) {
            emailService.sendVerificationEmail(user.getEmail(), rawToken);
        } else {
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        }
    }

    private AuthActionToken findActiveToken(String rawToken, AuthActionType purpose, OffsetDateTime now) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid or expired token");
        }
        Optional<AuthActionToken> maybeToken = tokenRepository.findByTokenHashAndPurposeForUpdate(
                hash(rawToken),
                purpose
        );
        AuthActionToken token = maybeToken
                .filter(candidate -> candidate.isActiveAt(now))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid or expired token"));
        return token;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void rejectCompromisedPassword(String password) {
        if (compromisedPasswordService.isCompromised(password)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Choose a password that has not appeared in known data breaches"
            );
        }
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
