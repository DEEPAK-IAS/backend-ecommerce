package com.example.ecommerce.security.jwt;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Creates and verifies short-lived access tokens (signed, not encrypted: anyone can read the claims,
 * so they contain only the user id, role and timestamps, never personal data).
 */
@Component
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Clock clock;
    private final String issuer;
    private final long accessExpirationSeconds;

    public JwtService(JwtProperties properties, Clock clock) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.clock = clock;
        this.issuer = properties.issuer();
        this.accessExpirationSeconds = properties.accessExpirationSeconds();
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
                .signWith(key)
                .compact();
    }

    /** @throws InvalidTokenException if the token is malformed, tampered with, expired or from another issuer */
    public AccessTokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AccessTokenClaims(
                    UUID.fromString(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
        } catch (RuntimeException e) {
            // Any failure (signature, expiry, format, missing claim) means "not a valid token".
            throw new InvalidTokenException("Invalid or expired access token");
        }
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }
}
