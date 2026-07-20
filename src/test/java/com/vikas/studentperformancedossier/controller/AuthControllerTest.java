package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.AuthResponse;
import com.vikas.studentperformancedossier.dto.LoginRequest;
import com.vikas.studentperformancedossier.dto.RegisterRequest;
import com.vikas.studentperformancedossier.dto.UserResponse;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.exception.InvalidCredentialsException;
import com.vikas.studentperformancedossier.exception.InvalidRequestException;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void register_whenValid_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("ada", "password123", Role.TEACHER, null);
        when(authService.register(eq(request))).thenReturn(sampleUserResponse("ada", Role.TEACHER, null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void register_whenUsernameBlank_returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "password123", Role.TEACHER, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void register_whenPasswordTooShort_returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("ada", "short", Role.TEACHER, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_whenRoleMissing_returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("ada", "password123", null, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void register_whenUsernameAlreadyExists_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("ada", "password123", Role.TEACHER, null);
        when(authService.register(eq(request)))
                .thenThrow(new DuplicateResourceException("A user with username 'ada' already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A user with username 'ada' already exists"));
    }

    @Test
    void register_whenStudentRoleWithValidStudentId_returns201WithStudentId() throws Exception {
        RegisterRequest request = new RegisterRequest("ada", "password123", Role.STUDENT, 5L);
        when(authService.register(eq(request))).thenReturn(sampleUserResponse("ada", Role.STUDENT, 5L));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentId").value(5));
    }

    @Test
    void register_whenStudentRoleMissingStudentId_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("ada", "password123", Role.STUDENT, null);
        when(authService.register(eq(request)))
                .thenThrow(new InvalidRequestException("studentId is required when role is STUDENT"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("studentId is required when role is STUDENT"));
    }

    @Test
    void register_whenStudentRoleInvalidStudentId_returns404() throws Exception {
        RegisterRequest request = new RegisterRequest("ada", "password123", Role.STUDENT, 99L);
        when(authService.register(eq(request)))
                .thenThrow(new EntityNotFoundException("Student not found with id: 99"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 99"));
    }

    @Test
    void register_whenTeacherRoleWithStudentIdProvided_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("ada", "password123", Role.TEACHER, 5L);
        when(authService.register(eq(request)))
                .thenThrow(new InvalidRequestException("studentId must not be provided unless role is STUDENT"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("studentId must not be provided unless role is STUDENT"));
    }

    @Test
    void register_whenAdminRoleWithoutStudentId_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("grace", "password123", Role.ADMIN, null);
        when(authService.register(eq(request))).thenReturn(sampleUserResponse("grace", Role.ADMIN, null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.studentId").doesNotExist());
    }

    @Test
    void login_whenValid_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("ada", "password123");
        when(authService.login(eq(request))).thenReturn(new AuthResponse("jwt-token", "ada", Role.TEACHER));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void login_whenPasswordWrong_returns401() throws Exception {
        LoginRequest request = new LoginRequest("ada", "wrong-password");
        when(authService.login(eq(request)))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void login_whenUsernameBlank_returns400() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    private UserResponse sampleUserResponse(String username, Role role, Long studentId) {
        return new UserResponse(
                1L,
                username,
                role,
                studentId,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
