package com.syntaxmath.backend.auth.service;

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

    public void failedLogin(String email, String clientIp) {
        LOGGER.warn("auth.login.failed account={} sourceIp={}", accountIdentifier(email), clientIp);
    }

    public void accountLocked(String email, String clientIp) {
        LOGGER.warn("auth.login.account_locked account={} sourceIp={}", accountIdentifier(email), clientIp);
    }

    public void successfulLogin(String email, String clientIp) {
        LOGGER.info("auth.login.succeeded account={} sourceIp={}", accountIdentifier(email), clientIp);
    }

    public void emailVerified(String email) {
        LOGGER.info("auth.email.verified account={}", accountIdentifier(email));
    }

    public void passwordReset(String email) {
        LOGGER.info("auth.password.reset account={}", accountIdentifier(email));
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
