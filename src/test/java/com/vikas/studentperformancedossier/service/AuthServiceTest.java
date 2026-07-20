package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.AuthResponse;
import com.vikas.studentperformancedossier.dto.LoginRequest;
import com.vikas.studentperformancedossier.dto.RegisterRequest;
import com.vikas.studentperformancedossier.dto.UserResponse;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.exception.InvalidCredentialsException;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("ada", "password123", Role.TEACHER);
        loginRequest = new LoginRequest("ada", "password123");
    }

    @Test
    void register_whenUsernameAlreadyExists_throwsDuplicateResourceException() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(existingUser(2L, "ada", "hashed")));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ada");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenUsernameAvailable_hashesPasswordAndSavesUser() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser(1L, "ada", "hashed-password"));

        UserResponse response = authService.register(registerRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("ada");
        assertThat(response.role()).isEqualTo(Role.TEACHER);
    }

    @Test
    void login_whenUsernameNotFound_throwsInvalidCredentialsException() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenPasswordWrong_throwsInvalidCredentialsException() {
        User user = existingUser(1L, "ada", "hashed-password");
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenCredentialsValid_returnsAuthResponseWithToken() {
        User user = existingUser(1L, "ada", "hashed-password");
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("ada", Role.TEACHER)).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("ada");
        assertThat(response.role()).isEqualTo(Role.TEACHER);
    }

    private User existingUser(Long id, String username, String hashedPassword) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole(Role.TEACHER);
        return user;
    }
}
