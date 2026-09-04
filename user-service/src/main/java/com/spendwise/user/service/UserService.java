package com.spendwise.user.service;

import com.spendwise.user.dto.AuthResponse;
import com.spendwise.user.dto.LoginRequest;
import com.spendwise.user.dto.RegisterRequest;
import com.spendwise.user.dto.UserResponse;
import com.spendwise.user.entity.RefreshToken;
import com.spendwise.user.entity.User;
import com.spendwise.user.exception.EmailAlreadyExistsException;
import com.spendwise.user.exception.InvalidCredentialsException;
import com.spendwise.user.exception.InvalidRefreshTokenException;
import com.spendwise.user.repository.RefreshTokenRepository;
import com.spendwise.user.repository.UserRepository;
import com.spendwise.user.security.JwtService;
import com.spendwise.user.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final long refreshTokenExpirationMs;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            TokenHasher tokenHasher,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), hashedPassword);
        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String rawRefreshToken = issueRefreshToken(user.getId(), deviceInfo, ipAddress);

        return new AuthResponse(accessToken, rawRefreshToken, toResponse(user));
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String deviceInfo, String ipAddress) {
        String tokenHash = tokenHasher.hash(rawRefreshToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        if (existing.isRevoked()) {
            // Reuse of a revoked/rotated token is a strong signal of theft.
            // Revoke every other active token for this user as a precaution.
            revokeAllTokensForUser(existing.getUserId());
            throw new InvalidRefreshTokenException("Refresh token has already been used. All sessions revoked for safety.");
        }

        if (existing.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));

        // Rotation: issue a new refresh token, mark the old one as replaced.
        String newRawRefreshToken = jwtService.generateOpaqueRefreshToken();
        String newTokenHash = tokenHasher.hash(newRawRefreshToken);

        existing.markReplacedBy(newTokenHash);
        refreshTokenRepository.save(existing);

        RefreshToken newToken = new RefreshToken(
                user.getId(),
                newTokenHash,
                deviceInfo,
                ipAddress,
                LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000)
        );
        refreshTokenRepository.save(newToken);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());

        return new AuthResponse(newAccessToken, newRawRefreshToken, toResponse(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = tokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
        // Intentionally silent if not found - logout should be idempotent,
        // not leak whether a given token was valid.
    }

    public UserResponse getById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
        return toResponse(user);
    }

    private String issueRefreshToken(UUID userId, String deviceInfo, String ipAddress) {
        String rawToken = jwtService.generateOpaqueRefreshToken();
        String tokenHash = tokenHasher.hash(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                userId,
                tokenHash,
                deviceInfo,
                ipAddress,
                LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000)
        );
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private void revokeAllTokensForUser(UUID userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)
                .forEach(RefreshToken::revoke);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt()
        );
    }
}