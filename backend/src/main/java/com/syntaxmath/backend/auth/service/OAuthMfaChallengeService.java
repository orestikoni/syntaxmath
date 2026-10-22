package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.entity.OAuthMfaChallenge;
import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.interfaces.OAuthMfaChallengeRepository;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class OAuthMfaChallengeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long CHALLENGE_LIFETIME_MINUTES = 5;

    private final OAuthMfaChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final TotpMfaService totpMfaService;

    public OAuthMfaChallengeService(OAuthMfaChallengeRepository challengeRepository,
                                    UserRepository userRepository,
                                    TotpMfaService totpMfaService) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.totpMfaService = totpMfaService;
    }

    @Transactional
    public String createChallenge(UUID userId) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        challengeRepository.save(new OAuthMfaChallenge(
                userId,
                hash(rawToken),
                OffsetDateTime.now().plusMinutes(CHALLENGE_LIFETIME_MINUTES)
        ));
        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<User> findChallengeUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        OffsetDateTime now = OffsetDateTime.now();
        return challengeRepository.findByTokenHash(hash(rawToken))
                .filter(challenge -> challenge.isActiveAt(now))
                .flatMap(challenge -> userRepository.findById(challenge.getUserId()))
                .filter(User::isMfaEnabled);
    }

    @Transactional
    public User completeChallenge(String rawToken, String code) {
        OffsetDateTime now = OffsetDateTime.now();
        OAuthMfaChallenge challenge = rawToken == null || rawToken.isBlank()
                ? null
                : challengeRepository.findByTokenHashForUpdate(hash(rawToken))
                .filter(candidate -> candidate.isActiveAt(now))
                .orElse(null);
        if (challenge == null) {
            throw invalidChallenge();
        }

        User user = userRepository.findByIdForUpdate(challenge.getUserId())
                .filter(User::isMfaEnabled)
                .orElseThrow(this::invalidChallenge);
        if (!totpMfaService.verifyLoginCode(user, code)) {
            throw invalidChallenge();
        }

        challenge.consume(now);
        challengeRepository.save(challenge);
        return user;
    }

    private ResponseStatusException invalidChallenge() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired sign-in challenge");
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
