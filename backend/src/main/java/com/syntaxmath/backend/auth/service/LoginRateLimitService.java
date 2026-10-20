package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class LoginRateLimitService {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local ip_count = redis.call('INCR', KEYS[1])
            if ip_count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            elseif redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end

            local account_count = redis.call('INCR', KEYS[2])
            if account_count == 1 then
                redis.call('EXPIRE', KEYS[2], ARGV[1])
            elseif redis.call('TTL', KEYS[2]) < 0 then
                redis.call('EXPIRE', KEYS[2], ARGV[1])
            end

            local retry_after = 0
            if ip_count > tonumber(ARGV[2]) then
                retry_after = math.max(retry_after, redis.call('TTL', KEYS[1]))
            end
            if account_count > tonumber(ARGV[3]) then
                retry_after = math.max(retry_after, redis.call('TTL', KEYS[2]))
            end
            if retry_after > 0 then
                return retry_after
            end
            if ip_count > tonumber(ARGV[2]) or account_count > tonumber(ARGV[3]) then
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    public LoginRateLimitService(StringRedisTemplate redisTemplate, AuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    public Duration checkAndRecordAttempt(String clientIp, String email) {
        Duration window = authProperties.getLoginRateLimitWindow();
        int windowSeconds = Math.toIntExact(window.toSeconds());
        if (windowSeconds <= 0
                || authProperties.getLoginRateLimitIpAttempts() <= 0
                || authProperties.getLoginRateLimitAccountAttempts() <= 0) {
            throw new IllegalStateException("Login rate limit settings must be positive");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        List<String> keys = List.of(
                "auth:login:ip:" + hash(clientIp),
                "auth:login:account:" + hash(normalizedEmail)
        );

        try {
            Long retryAfterSeconds = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    keys,
                    Integer.toString(windowSeconds),
                    Integer.toString(authProperties.getLoginRateLimitIpAttempts()),
                    Integer.toString(authProperties.getLoginRateLimitAccountAttempts())
            );
            if (retryAfterSeconds == null) {
                throw new IllegalStateException("Redis returned no login rate-limit result");
            }
            return retryAfterSeconds > 0 ? Duration.ofSeconds(retryAfterSeconds) : Duration.ZERO;
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Login is temporarily unavailable",
                    exception
            );
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
