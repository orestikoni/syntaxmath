package com.syntaxmath.backend.auth.interfaces;

import com.syntaxmath.backend.auth.entity.AuthSecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuthSecurityEventRepository extends JpaRepository<AuthSecurityEvent, UUID> {

    @Modifying
    @Query("delete from AuthSecurityEvent event where event.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
