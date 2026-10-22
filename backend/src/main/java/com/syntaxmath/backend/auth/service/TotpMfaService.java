package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.config.AuthProperties;
import com.syntaxmath.backend.auth.dto.MfaEnrollmentResponse;
import com.syntaxmath.backend.auth.dto.MfaRecoveryCodesResponse;
import com.syntaxmath.backend.auth.entity.MfaRecoveryCode;
import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.interfaces.MfaRecoveryCodeRepository;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;

@Service
public class TotpMfaService {

    private static final String ISSUER = "SyntaxMath";
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final long TOTP_STEP_SECONDS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;
    private final AuthSecurityEventLogger securityEventLogger;

    public TotpMfaService(AuthProperties authProperties,
                          UserRepository userRepository,
                          MfaRecoveryCodeRepository recoveryCodeRepository,
                          AuthSecurityEventLogger securityEventLogger) {
        this.authProperties = authProperties;
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.securityEventLogger = securityEventLogger;
    }

    @Transactional
    public MfaEnrollmentResponse beginEnrollment(User user) {
        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is already enabled");
        }
        byte[] secretBytes = new byte[20];
        SECURE_RANDOM.nextBytes(secretBytes);
        String secret = base32Encode(secretBytes);
        user.setPendingTotpSecret(encrypt(secret));
        userRepository.save(user);

        String label = URLEncoder.encode(ISSUER + ":" + user.getEmail(), StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        String uri = "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
        return new MfaEnrollmentResponse(secret, uri);
    }

    @Transactional
    public MfaRecoveryCodesResponse enable(User user, String code) {
        if (user.isMfaEnabled() || user.getTotpSecretEncrypted() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Start MFA enrollment first");
        }
        String secret = decrypt(user.getTotpSecretEncrypted());
        OptionalLong matchedStep = matchingTotpStep(secret, code, null);
        if (matchedStep.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authenticator code");
        }

        recoveryCodeRepository.deleteAllByUserId(user.getId());
        List<String> rawCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        List<MfaRecoveryCode> storedCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int index = 0; index < RECOVERY_CODE_COUNT; index++) {
            byte[] bytes = new byte[9];
            SECURE_RANDOM.nextBytes(bytes);
            String recoveryCode = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            rawCodes.add(recoveryCode);
            storedCodes.add(new MfaRecoveryCode(user.getId(), hash(recoveryCode)));
        }
        recoveryCodeRepository.saveAll(storedCodes);
        user.enableMfa();
        user.markTotpStepUsed(matchedStep.getAsLong());
        userRepository.save(user);
        securityEventLogger.mfaEnabled(user.getEmail());
        return new MfaRecoveryCodesResponse(List.copyOf(rawCodes));
    }

    @Transactional
    public boolean verifyLoginCode(User user, String code) {
        if (!user.isMfaEnabled() || code == null || code.isBlank()) {
            return false;
        }
        User managedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        if (!managedUser.isMfaEnabled()) {
            return false;
        }
        OptionalLong matchedStep = matchingTotpStep(
                decrypt(managedUser.getTotpSecretEncrypted()),
                code,
                managedUser.getTotpLastUsedStep()
        );
        if (matchedStep.isPresent()) {
            managedUser.markTotpStepUsed(matchedStep.getAsLong());
            userRepository.save(managedUser);
            return true;
        }
        boolean recoveryCodeUsed = recoveryCodeRepository.consumeByUserIdAndCodeHash(
                managedUser.getId(),
                hash(code.trim())
        ) == 1;
        if (recoveryCodeUsed) {
            securityEventLogger.recoveryCodeUsed(managedUser.getEmail());
        }
        return recoveryCodeUsed;
    }

    @Transactional
    public void disable(User user, String code) {
        if (!user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is not enabled");
        }
        if (!verifyLoginCode(user, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authenticator or recovery code");
        }
        recoveryCodeRepository.deleteAllByUserId(user.getId());
        user.disableMfa();
        userRepository.save(user);
        securityEventLogger.mfaDisabled(user.getEmail());
    }

    private OptionalLong matchingTotpStep(String secret, String suppliedCode, Long lastUsedStep) {
        if (suppliedCode == null || !suppliedCode.matches("\\d{6}")) {
            return OptionalLong.empty();
        }
        long currentStep = Instant.now().getEpochSecond() / TOTP_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            long candidateStep = currentStep + offset;
            if (candidateStep < 0 || (lastUsedStep != null && candidateStep <= lastUsedStep)) {
                continue;
            }
            String expectedCode = totpCode(base32Decode(secret), candidateStep);
            if (MessageDigest.isEqual(
                    expectedCode.getBytes(StandardCharsets.US_ASCII),
                    suppliedCode.getBytes(StandardCharsets.US_ASCII)
            )) {
                return OptionalLong.of(candidateStep);
            }
        }
        return OptionalLong.empty();
    }

    private String totpCode(byte[] secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
            int offset = digest[digest.length - 1] & 0x0f;
            int binary = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("TOTP cryptography is unavailable", exception);
        }
    }

    private String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[12];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + ciphertext.length)
                    .put(nonce)
                    .put(ciphertext)
                    .array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt TOTP secret", exception);
        }
    }

    private String decrypt(String encrypted) {
        try {
            byte[] value = Base64.getDecoder().decode(encrypted);
            if (value.length <= 12) {
                throw new IllegalStateException("Stored TOTP secret is invalid");
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] nonce = new byte[12];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt TOTP secret", exception);
        }
    }

    private SecretKeySpec encryptionKey() {
        String configuredKey = authProperties.getMfaEncryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MFA encryption is not configured"
            );
        }
        try {
            byte[] key = Base64.getDecoder().decode(configuredKey);
            if (key.length != 32) {
                throw new IllegalStateException("MFA_ENCRYPTION_KEY must decode to exactly 32 bytes");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY must be valid Base64", exception);
        }
    }

    private String base32Encode(byte[] bytes) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(alphabet[(buffer >> (bitsLeft - 5)) & 0x1f]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(alphabet[(buffer << (5 - bitsLeft)) & 0x1f]);
        }
        return result.toString();
    }

    private byte[] base32Decode(String value) {
        String normalized = value.replace("=", "").toUpperCase(Locale.ROOT);
        byte[] result = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char character : normalized.toCharArray()) {
            int digit = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(character);
            if (digit < 0) {
                throw new IllegalStateException("Stored TOTP secret is invalid");
            }
            buffer = (buffer << 5) | digit;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
