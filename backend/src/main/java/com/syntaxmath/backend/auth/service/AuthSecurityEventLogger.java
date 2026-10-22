package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.entity.AuthSecurityEvent;
import com.syntaxmath.backend.auth.interfaces.AuthSecurityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class AuthSecurityEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthSecurityEventLogger.class);

    private final AuthSecurityEventRepository eventRepository;

    public AuthSecurityEventLogger(AuthSecurityEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void failedLogin(String email, String clientIp) {
        record("auth.login.failed", email, null);
        LOGGER.warn("auth.login.failed account={} sourceIp={}", accountIdentifier(email), clientIp);
    }

    public void accountLocked(String email, String clientIp) {
        record("auth.login.account_locked", email, null);
        LOGGER.warn("auth.login.account_locked account={} sourceIp={}", accountIdentifier(email), clientIp);
    }

    public void successfulLogin(String email, String clientIp) {
        record("auth.login.succeeded", email, null);
        LOGGER.info("auth.login.succeeded account={} sourceIp={}", accountIdentifier(email), clientIp);
    }

    public void emailVerified(String email) {
        record("auth.email.verified", email, null);
        LOGGER.info("auth.email.verified account={}", accountIdentifier(email));
    }

    public void passwordReset(String email) {
        record("auth.password.reset", email, null);
        LOGGER.info("auth.password.reset account={}", accountIdentifier(email));
    }

    public void sessionsRevoked(String email, int count, String scope) {
        record("auth.sessions.revoked", email, "count=" + count + ";scope=" + scope);
        LOGGER.info("auth.sessions.revoked account={} count={} scope={}",
                accountIdentifier(email), count, scope);
    }

    public void mfaEnabled(String email) {
        record("auth.mfa.enabled", email, null);
        LOGGER.info("auth.mfa.enabled account={}", accountIdentifier(email));
    }

    public void mfaDisabled(String email) {
        record("auth.mfa.disabled", email, null);
        LOGGER.warn("auth.mfa.disabled account={}", accountIdentifier(email));
    }

    public void recoveryCodeUsed(String email) {
        record("auth.mfa.recovery_code_used", email, null);
        LOGGER.warn("auth.mfa.recovery_code_used account={}", accountIdentifier(email));
    }

    public void registrationCompleted(String email) {
        record("auth.registration.completed", email, null);
        LOGGER.info("auth.registration.completed account={}", accountIdentifier(email));
    }

    public void oauthMfaChallengeStarted(String email) {
        record("auth.oauth.mfa_challenge_started", email, null);
        LOGGER.info("auth.oauth.mfa_challenge_started account={}", accountIdentifier(email));
    }

    private void record(String eventType, String email, String details) {
        eventRepository.saveAndFlush(new AuthSecurityEvent(
                eventType,
                accountIdentifier(email),
                details
        ));
    }

    private String accountIdentifier(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
