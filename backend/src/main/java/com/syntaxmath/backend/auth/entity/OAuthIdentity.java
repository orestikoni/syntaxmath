package com.syntaxmath.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "oauth_identities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth_identity_provider_subject", columnNames = {"provider", "provider_subject"}),
                @UniqueConstraint(name = "uk_oauth_identity_provider_user", columnNames = {"provider", "user_id"})
        }
)
public class OAuthIdentity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected OAuthIdentity() {
    }

    public OAuthIdentity(UUID userId, String provider, String providerSubject) {
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
    }

    public UUID getUserId() {
        return userId;
    }
}
