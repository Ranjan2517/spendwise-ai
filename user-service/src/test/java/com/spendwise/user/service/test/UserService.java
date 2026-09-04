package com.spendwise.user.service.test;
import com.spendwise.user.dto.LoginRequest;
import com.spendwise.user.dto.RegisterRequest;
import com.spendwise.user.entity.User;
import com.spendwise.user.exception.EmailAlreadyExistsException;
import com.spendwise.user.exception.InvalidCredentialsException;
import com.spendwise.user.repository.RefreshTokenRepository;
import com.spendwise.user.repository.UserRepository;
import com.spendwise.user.security.JwtService;
import com.spendwise.user.security.TokenHasher;
import com.spendwise.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenHasher tokenHasher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                refreshTokenRepository,
                jwtService,
                tokenHasher,
                604800000L
        );
    }

    @Test
    void register_savesNewUser_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.register(request);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_throwsInvalidCredentials_whenEmailNotFound() {
        LoginRequest request = new LoginRequest("ghost@example.com", "password123");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request, "test-device", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordWrong() {
        User existingUser = new User("user@example.com", encodedPassword("correct-password"));
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.login(request, "test-device", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_returnsTokens_whenCredentialsCorrect() {
        String rawPassword = "correct-password";
        User existingUser = new User("user@example.com", encodedPassword(rawPassword));
        LoginRequest request = new LoginRequest("user@example.com", rawPassword);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("fake-access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("fake-refresh-token");
        when(tokenHasher.hash("fake-refresh-token")).thenReturn("hashed-refresh-token");

        var response = userService.login(request, "test-device", "127.0.0.1");

        assertThat(response.getAccessToken()).isEqualTo("fake-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("fake-refresh-token");
        verify(refreshTokenRepository).save(any());
    }

    private String encodedPassword(String rawPassword) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
    }
}