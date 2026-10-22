package com.syntaxmath.backend.auth.interfaces;

import com.syntaxmath.backend.auth.entity.MfaRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

    @Modifying
    @Query("delete from MfaRecoveryCode code where code.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from MfaRecoveryCode code where code.userId = :userId and code.codeHash = :codeHash")
    int consumeByUserIdAndCodeHash(
            @Param("userId") UUID userId,
            @Param("codeHash") String codeHash
    );
}
