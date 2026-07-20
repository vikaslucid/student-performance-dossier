package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.AuthResponse;
import com.vikas.studentperformancedossier.dto.LoginRequest;
import com.vikas.studentperformancedossier.dto.RegisterRequest;
import com.vikas.studentperformancedossier.dto.UserResponse;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.exception.InvalidCredentialsException;
import com.vikas.studentperformancedossier.exception.InvalidRequestException;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
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
    private StudentRepository studentRepository;

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
        registerRequest = new RegisterRequest("ada", "password123", Role.TEACHER, null);
        loginRequest = new LoginRequest("ada", "password123");
    }

    @Test
    void register_whenUsernameAlreadyExists_throwsDuplicateResourceException() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(existingUser(2L, "ada", "hashed", Role.TEACHER, null)));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ada");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenTeacherRole_savesUserWithoutStudentLink() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser(1L, "ada", "hashed-password", Role.TEACHER, null));

        UserResponse response = authService.register(registerRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("ada");
        assertThat(response.role()).isEqualTo(Role.TEACHER);
        assertThat(response.studentId()).isNull();
    }

    @Test
    void register_whenAdminRole_savesUserWithoutStudentLink() {
        RegisterRequest adminRequest = new RegisterRequest("grace", "password123", Role.ADMIN, null);
        when(userRepository.findByUsername("grace")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser(1L, "grace", "hashed-password", Role.ADMIN, null));

        UserResponse response = authService.register(adminRequest);

        assertThat(response.studentId()).isNull();
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void register_whenTeacherRoleWithStudentIdProvided_throwsInvalidRequestException() {
        RegisterRequest invalidRequest = new RegisterRequest("ada", "password123", Role.TEACHER, 1L);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(invalidRequest))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenStudentRoleWithoutStudentId_throwsInvalidRequestException() {
        RegisterRequest invalidRequest = new RegisterRequest("ada", "password123", Role.STUDENT, null);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(invalidRequest))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenStudentRoleWithInvalidStudentId_throwsEntityNotFoundException() {
        RegisterRequest invalidRequest = new RegisterRequest("ada", "password123", Role.STUDENT, 99L);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(invalidRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenStudentIdAlreadyLinkedToAnotherUser_throwsDuplicateResourceException() {
        RegisterRequest studentRequest = new RegisterRequest("ada", "password123", Role.STUDENT, 5L);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(studentRepository.findById(5L)).thenReturn(Optional.of(existingStudent(5L)));
        when(userRepository.findByStudent_Id(5L)).thenReturn(Optional.of(existingUser(2L, "other", "hashed", Role.STUDENT, existingStudent(5L))));

        assertThatThrownBy(() -> authService.register(studentRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("5");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenStudentRoleWithValidStudentId_savesUserWithLinkedStudent() {
        RegisterRequest studentRequest = new RegisterRequest("ada", "password123", Role.STUDENT, 5L);
        Student student = existingStudent(5L);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(userRepository.findByStudent_Id(5L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser(1L, "ada", "hashed-password", Role.STUDENT, student));

        UserResponse response = authService.register(studentRequest);

        assertThat(response.studentId()).isEqualTo(5L);
        assertThat(response.role()).isEqualTo(Role.STUDENT);
    }

    @Test
    void login_whenUsernameNotFound_throwsInvalidCredentialsException() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenPasswordWrong_throwsInvalidCredentialsException() {
        User user = existingUser(1L, "ada", "hashed-password", Role.TEACHER, null);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenCredentialsValid_returnsAuthResponseWithToken() {
        User user = existingUser(1L, "ada", "hashed-password", Role.TEACHER, null);
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("ada", Role.TEACHER)).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("ada");
        assertThat(response.role()).isEqualTo(Role.TEACHER);
    }

    private User existingUser(Long id, String username, String hashedPassword, Role role, Student student) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole(role);
        user.setStudent(student);
        return user;
    }

    private Student existingStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("Ada");
        student.setLastName("Lovelace");
        student.setEmail("ada@example.com");
        student.setDateOfBirth(LocalDate.of(1990, 1, 1));
        student.setEnrollmentDate(LocalDate.of(2020, 1, 1));
        student.setStudentNumber("S-100");
        return student;
    }
}
