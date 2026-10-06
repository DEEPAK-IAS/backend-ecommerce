package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RefreshTokenRequest;
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
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    /** Compared against when the email is unknown, so "no such user" costs the same time as "wrong password". */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already registered");
        }

        // Role and status are fixed here. Nothing in the request can influence them.
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        log.info("User registered userId={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(normalizeEmail(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);

        boolean passwordMatches = matches(request.password(), hash);
        if (found.isEmpty() || !passwordMatches) {
            // Same message for both cases: the client cannot tell which emails exist.
            log.warn("Failed login attempt: {}", found.isEmpty() ? "unknown account" : "wrong password");
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (user.getStatus() != AccountStatus.ACTIVE) {
            // Only reached with the correct password, so this reveals nothing to a guesser.
            log.warn("Login blocked for suspended account userId={}", user.getId());
            throw new ForbiddenException("Account is suspended");
        }

        log.info("User logged in userId={}", user.getId());
        return AuthResponse.bearer(
                jwtService.generateAccessToken(user),
                refreshTokenService.issueNewFamily(user),
                jwtService.getAccessExpirationSeconds());
    }

    /**
     * Deliberately NOT @Transactional: RefreshTokenService.rotate owns the transaction. An outer
     * transaction would be marked rollback-only by the InvalidTokenException and undo the revocation.
     */
    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        return AuthResponse.bearer(
                jwtService.generateAccessToken(rotation.user()),
                rotation.newRefreshToken(),
                jwtService.getAccessExpirationSeconds());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeSessionOf(request.refreshToken());
    }

    private boolean matches(String rawPassword, String hash) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            return false; // can never match a stored password, and BCrypt cannot hash it anyway
        }
        return passwordEncoder.matches(rawPassword, hash);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
