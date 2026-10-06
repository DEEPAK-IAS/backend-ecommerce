package com.example.ecommerce.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.InvalidTokenException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234567890";
    private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

    private JwtService serviceAt(Instant time, String secret) {
        return new JwtService(new JwtProperties(secret, "ecommerce-api", 900, 604800),
                Clock.fixed(time, ZoneOffset.UTC));
    }

    private User user(Role role) {
        User user = User.builder().email("a@b.com").role(role).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @Test
    void generatedTokenParsesBackToSameUserAndRole() {
        JwtService service = serviceAt(NOW, SECRET);
        User user = user(Role.ADMIN);

        AccessTokenClaims claims = service.parse(service.generateAccessToken(user));

        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(NOW, SECRET).generateAccessToken(user(Role.USER));

        JwtService sixteenMinutesLater = serviceAt(NOW.plusSeconds(16 * 60), SECRET);

        assertThatThrownBy(() -> sixteenMinutesLater.parse(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String forged = serviceAt(NOW, "another-secret-another-secret-0987654321").generateAccessToken(user(Role.USER));

        assertThatThrownBy(() -> serviceAt(NOW, SECRET).parse(forged)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> serviceAt(NOW, SECRET).parse("not.a.jwt")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> serviceAt(NOW, "too-short")).isInstanceOf(IllegalStateException.class);
    }
}
