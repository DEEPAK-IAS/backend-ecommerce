package com.example.ecommerce.service;

import com.example.ecommerce.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expired tokens are already unusable (checked on every refresh); this only keeps the table small.
 * Safe with several app instances: the delete is idempotent.
 */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void purgeExpiredTokens() {
        Instant cutoff = clock.instant().minus(7, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Purged {} expired refresh tokens", deleted);
    }
}
