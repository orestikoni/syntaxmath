package com.syntaxmath.backend.auth.interfaces;

import com.syntaxmath.backend.auth.entity.AuthActionToken;
import com.syntaxmath.backend.auth.entity.AuthActionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken, UUID> {

    List<AuthActionToken> findAllByUserIdAndPurposeAndConsumedAtIsNull(UUID userId, AuthActionType purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AuthActionToken token where token.tokenHash = :tokenHash and token.purpose = :purpose")
    Optional<AuthActionToken> findByTokenHashAndPurposeForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") AuthActionType purpose
    );
}
