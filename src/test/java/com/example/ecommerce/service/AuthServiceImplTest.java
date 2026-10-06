package com.example.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.AccountStatus;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ForbiddenException;
import com.example.ecommerce.exception.UnauthorizedException;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.jwt.JwtService;
import com.example.ecommerce.service.impl.AuthServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService,
                refreshTokenService, new UserMapper());
    }

    private RegisterRequest registerRequest(String confirm) {
        return new RegisterRequest("Deepak", "  Deepak@Example.com ", "Passw0rd123", confirm, "+919876543210");
    }

    private User storedUser(AccountStatus status) {
        User user = User.builder().name("Deepak").email("deepak@example.com").phone("+919876543210")
                .passwordHash("hashed").role(Role.USER).status(status).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @Test
    void registerCreatesActiveUserRoleWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmail("deepak@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919876543210")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(registerRequest("Passw0rd123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getEmail()).isEqualTo("deepak@example.com");
        assertThat(response.email()).isEqualTo("deepak@example.com");
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        assertThatThrownBy(() -> authService.register(registerRequest("Different1")))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("deepak@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("Passw0rd123")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicatePhone() {
        when(userRepository.existsByEmail("deepak@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919876543210")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("Passw0rd123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginReturnsBearerTokens() {
        User user = storedUser(AccountStatus.ACTIVE);
        when(userRepository.findByEmail("deepak@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.issueNewFamily(user)).thenReturn("refresh");

        AuthResponse response = authService.login(new LoginRequest("Deepak@Example.com", "Passw0rd123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    void loginWithWrongPasswordIsUnauthorizedAndIssuesNothing() {
        when(userRepository.findByEmail("deepak@example.com")).thenReturn(Optional.of(storedUser(AccountStatus.ACTIVE)));
        when(passwordEncoder.matches("WrongPass1", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("deepak@example.com", "WrongPass1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginWithUnknownEmailGivesSameErrorAsWrongPassword() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "Passw0rd123")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void suspendedAccountCannotLogIn() {
        when(userRepository.findByEmail("deepak@example.com")).thenReturn(Optional.of(storedUser(AccountStatus.SUSPENDED)));
        when(passwordEncoder.matches("Passw0rd123", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("deepak@example.com", "Passw0rd123")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(refreshTokenService);
    }
}
