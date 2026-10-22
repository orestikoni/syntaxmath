package com.syntaxmath.backend.auth.service;

import com.syntaxmath.backend.auth.interfaces.AuthSecurityEventRepository;
import com.syntaxmath.backend.auth.interfaces.OAuthMfaChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SessionCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleanupService.class);

    private final SessionService sessionService;
    private final OAuthMfaChallengeRepository oAuthMfaChallengeRepository;
    private final AuthSecurityEventRepository authSecurityEventRepository;

    public SessionCleanupService(SessionService sessionService,
                                 OAuthMfaChallengeRepository oAuthMfaChallengeRepository,
                                 AuthSecurityEventRepository authSecurityEventRepository) {
        this.sessionService = sessionService;
        this.oAuthMfaChallengeRepository = oAuthMfaChallengeRepository;
        this.authSecurityEventRepository = authSecurityEventRepository;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void removeInactiveSessions() {
        int removedCount = sessionService.cleanupInactiveSessions();
        OffsetDateTime now = OffsetDateTime.now();
        int removedChallengeCount = oAuthMfaChallengeRepository.deleteInactive(now);
        int removedAuditEventCount = authSecurityEventRepository.deleteOlderThan(
                now.minusDays(365)
        );
        if (removedCount > 0) {
            LOGGER.info("auth.sessions.cleanup removed={}", removedCount);
        }
        if (removedChallengeCount > 0) {
            LOGGER.info("auth.mfa.oauth_challenges.cleanup removed={}", removedChallengeCount);
        }
        if (removedAuditEventCount > 0) {
            LOGGER.info("auth.security_events.cleanup removed={}", removedAuditEventCount);
        }
    }
}
