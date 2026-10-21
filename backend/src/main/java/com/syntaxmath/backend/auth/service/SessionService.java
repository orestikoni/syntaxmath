package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import com.syntaxmath.backend.auth.dto.SessionResponse;
import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.entity.UserSession;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import com.syntaxmath.backend.auth.interfaces.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(UserSessionRepository userSessionRepository,
                           UserRepository userRepository,
                           AuthProperties authProperties) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
        this.authProperties = authProperties;
    }

    public String createSession(UUID userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        UserSession session = new UserSession(
                userId,
                hash(rawToken),
                OffsetDateTime.now().plus(authProperties.getSessionLifetime())
        );
        userSessionRepository.save(session);

        return rawToken;
    }

    public Optional<User> validateSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return userSessionRepository.findByTokenHash(hash(rawToken))
                .filter(UserSession::isActive)
                .flatMap(session -> userRepository.findById(session.getUserId()));
    }

    public void revokeSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        userSessionRepository.findByTokenHash(hash(rawToken)).ifPresent(session -> {
            session.revoke();
            userSessionRepository.save(session);
        });
    }

    public int revokeAllSessions(UUID userId) {
        List<UserSession> activeSessions = userSessionRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        activeSessions.forEach(UserSession::revoke);
        userSessionRepository.saveAll(activeSessions);
        return activeSessions.size();
    }

    public List<SessionResponse> listActiveSessions(UUID userId, String currentRawToken) {
        String currentTokenHash = currentRawToken == null || currentRawToken.isBlank()
                ? null
                : hash(currentRawToken);
        return userSessionRepository
                .findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        OffsetDateTime.now()
                )
                .stream()
                .map(session -> new SessionResponse(
                        session.getId(),
                        session.getCreatedAt(),
                        session.getExpiresAt(),
                        currentTokenHash != null && currentTokenHash.equals(session.getTokenHash())
                ))
                .toList();
    }

    public Optional<UUID> findSessionIdForToken(UUID userId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return userSessionRepository.findByTokenHash(hash(rawToken))
                .filter(session -> session.getUserId().equals(userId) && session.isActive())
                .map(UserSession::getId);
    }

    public boolean revokeSession(UUID userId, UUID sessionId) {
        return userSessionRepository.findByIdAndUserId(sessionId, userId)
                .filter(UserSession::isActive)
                .map(session -> {
                    session.revoke();
                    userSessionRepository.save(session);
                    return true;
                })
                .orElse(false);
    }

    public int cleanupInactiveSessions() {
        return userSessionRepository.deleteInactiveSessions(OffsetDateTime.now());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
