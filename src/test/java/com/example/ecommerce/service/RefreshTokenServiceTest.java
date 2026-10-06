package com.example.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.entity.AccountStatus;
import com.example.ecommerce.entity.RefreshToken;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.InvalidTokenException;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.security.jwt.JwtProperties;
import com.example.ecommerce.util.TokenHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");
    private static final String RAW = "raw-refresh-token";

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private UUID familyId;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository,
                new JwtProperties("unit-test-secret-unit-test-secret-1234567890", "ecommerce-api", 900, 604800),
                Clock.fixed(NOW, ZoneOffset.UTC));
        familyId = UUID.randomUUID();
    }

    private User user(AccountStatus status) {
        User user = User.builder().email("a@b.com").role(Role.USER).status(status).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private RefreshToken token(User user, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(RAW))
                .familyId(familyId)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .build();
        when(repository.findByTokenHashWithUser(TokenHasher.sha256Hex(RAW))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void rotateRevokesOldTokenAndIssuesNewOneInSameFamily() {
        User user = user(AccountStatus.ACTIVE);
        RefreshToken old = token(user, NOW.plusSeconds(3600), null);
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate(RAW);

        assertThat(old.isRevoked()).isTrue();
        assertThat(rotation.user()).isSameAs(user);
        assertThat(rotation.newRefreshToken()).isNotBlank().isNotEqualTo(RAW);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(familyId);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256Hex(rotation.newRefreshToken()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(rotation.newRefreshToken()); // stored hashed
    }

    @Test
    void reusingARevokedTokenRevokesTheWholeFamily() {
        token(user(AccountStatus.ACTIVE), NOW.plusSeconds(3600), NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidTokenException.class);

        verify(repository).revokeFamily(familyId, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void expiredTokenIsRejected() {
        token(user(AccountStatus.ACTIVE), NOW.minusSeconds(1), null);

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidTokenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHashWithUser(TokenHasher.sha256Hex(RAW))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void suspendedUserCannotRefreshAndSessionIsRevoked() {
        token(user(AccountStatus.SUSPENDED), NOW.plusSeconds(3600), null);

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidTokenException.class);

        verify(repository).revokeFamily(familyId, NOW);
    }

    @Test
    void logoutRevokesTheFamilyAndIgnoresUnknownTokens() {
        RefreshToken existing = RefreshToken.builder().familyId(familyId).build();
        when(repository.findByTokenHash(TokenHasher.sha256Hex(RAW))).thenReturn(Optional.of(existing));

        service.revokeSessionOf(RAW);
        verify(repository).revokeFamily(familyId, NOW);

        when(repository.findByTokenHash(TokenHasher.sha256Hex("unknown"))).thenReturn(Optional.empty());
        service.revokeSessionOf("unknown"); // must not throw
    }
}
