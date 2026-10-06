package com.example.ecommerce.service;

import com.example.ecommerce.entity.AccountStatus;
import com.example.ecommerce.entity.RefreshToken;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.InvalidTokenException;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.security.jwt.JwtProperties;
import com.example.ecommerce.util.TokenHasher;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh-token lifecycle: issue, rotate, revoke.
 *
 * Every refresh produces a NEW token and revokes the old one (rotation). If a revoked token is ever
 * presented again, someone is replaying a stolen copy, so the whole family (session) is revoked.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID_MESSAGE = "Invalid refresh token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProperties jwtProperties,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    /** Result of a successful rotation. */
    public record Rotation(User user, String newRefreshToken) {
    }

    /** Starts a new session (family) at login and returns the raw token for the client. */
    @Transactional
    public String issueNewFamily(User user) {
        return createToken(user, UUID.randomUUID());
    }

    /**
     * noRollbackFor is essential: when reuse is detected we revoke the family and THEN throw.
     * With the default rollback rule that revocation would be undone and the attack would go unpunished.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository
                .findByTokenHashWithUser(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(() -> new InvalidTokenException(INVALID_MESSAGE));

        UUID familyId = current.getFamilyId();
        User user = current.getUser();

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected, revoking session userId={}", user.getId());
            refreshTokenRepository.revokeFamily(familyId, now);
            throw new InvalidTokenException(INVALID_MESSAGE);
        }
        if (current.isExpired(now)) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            refreshTokenRepository.revokeFamily(familyId, now);
            throw new InvalidTokenException(INVALID_MESSAGE);
        }

        current.revoke(now);
        String newRawToken = createToken(user, familyId);
        return new Rotation(user, newRawToken);
    }

    /** Logout: revokes the whole session. Unknown tokens are ignored so logout is idempotent. */
    @Transactional
    public void revokeSessionOf(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private String createToken(User user, UUID familyId) {
        String rawToken = generateRawToken();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(rawToken))
                .familyId(familyId)
                .expiresAt(clock.instant().plusSeconds(jwtProperties.refreshExpirationSeconds()))
                .build();
        refreshTokenRepository.save(token);
        return rawToken;
    }

    /** 256 random bits, URL-safe. */
    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
