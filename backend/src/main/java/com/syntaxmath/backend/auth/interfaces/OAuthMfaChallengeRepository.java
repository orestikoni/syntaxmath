package com.syntaxmath.backend.auth.interfaces;

import com.syntaxmath.backend.auth.entity.OAuthMfaChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface OAuthMfaChallengeRepository extends JpaRepository<OAuthMfaChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from OAuthMfaChallenge challenge where challenge.tokenHash = :tokenHash")
    Optional<OAuthMfaChallenge> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<OAuthMfaChallenge> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from OAuthMfaChallenge challenge where challenge.expiresAt <= :now or challenge.consumedAt is not null")
    int deleteInactive(@Param("now") OffsetDateTime now);
}
