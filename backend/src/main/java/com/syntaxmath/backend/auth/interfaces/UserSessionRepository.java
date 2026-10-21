package com.syntaxmath.backend.auth.interfaces;

import com.syntaxmath.backend.auth.entity.UserSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByTokenHash(String tokenHash);

    List<UserSession> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    List<UserSession> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            OffsetDateTime now
    );

    Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("delete from UserSession s where s.expiresAt <= :now or s.revokedAt is not null")
    int deleteInactiveSessions(@Param("now") OffsetDateTime now);
}
