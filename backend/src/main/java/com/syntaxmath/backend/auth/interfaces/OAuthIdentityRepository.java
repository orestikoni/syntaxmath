package com.syntaxmath.backend.auth.interfaces;

import com.syntaxmath.backend.auth.entity.OAuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {

    Optional<OAuthIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

    boolean existsByProviderAndUserId(String provider, UUID userId);
}
