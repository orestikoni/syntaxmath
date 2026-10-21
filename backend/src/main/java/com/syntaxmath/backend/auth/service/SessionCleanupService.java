package com.syntaxmath.backend.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleanupService.class);

    private final SessionService sessionService;

    public SessionCleanupService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void removeInactiveSessions() {
        int removedCount = sessionService.cleanupInactiveSessions();
        if (removedCount > 0) {
            LOGGER.info("auth.sessions.cleanup removed={}", removedCount);
        }
    }
}
