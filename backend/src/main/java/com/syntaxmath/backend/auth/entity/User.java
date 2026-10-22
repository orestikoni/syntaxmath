package com.syntaxmath.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "totp_secret_encrypted")
    private String totpSecretEncrypted;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "totp_last_used_step")
    private Long totpLastUsedStep;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected User() {
        // required by JPA
    }

    public User(String email, String passwordHash, String firstName, String lastName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTotpSecretEncrypted() {
        return totpSecretEncrypted;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setPendingTotpSecret(String encryptedSecret) {
        this.totpSecretEncrypted = encryptedSecret;
        this.mfaEnabled = false;
    }

    public void enableMfa() {
        if (totpSecretEncrypted == null || totpSecretEncrypted.isBlank()) {
            throw new IllegalStateException("A TOTP secret must be enrolled before enabling MFA");
        }
        this.mfaEnabled = true;
    }

    public Long getTotpLastUsedStep() {
        return totpLastUsedStep;
    }

    public void markTotpStepUsed(long step) {
        this.totpLastUsedStep = step;
    }

    public void disableMfa() {
        this.mfaEnabled = false;
        this.totpSecretEncrypted = null;
        this.totpLastUsedStep = null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
