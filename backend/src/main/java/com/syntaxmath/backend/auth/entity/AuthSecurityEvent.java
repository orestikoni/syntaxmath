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
@Table(name = "auth_security_events")
public class AuthSecurityEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "account_identifier_hash", nullable = false, length = 64)
    private String accountIdentifierHash;

    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AuthSecurityEvent() {
    }

    public AuthSecurityEvent(String eventType, String accountIdentifierHash, String eventDetails) {
        this.eventType = eventType;
        this.accountIdentifierHash = accountIdentifierHash;
        this.eventDetails = eventDetails;
    }
}
