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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, StudentRepository studentRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        userRepository.findByUsername(request.username())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A user with username '" + request.username() + "' already exists");
                });

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStudent(resolveLinkedStudent(request));

        return toResponse(userRepository.save(user));
    }

    private Student resolveLinkedStudent(RegisterRequest request) {
        if (request.role() != Role.STUDENT) {
            if (request.studentId() != null) {
                throw new InvalidRequestException("studentId must not be provided unless role is STUDENT");
            }
            return null;
        }

        if (request.studentId() == null) {
            throw new InvalidRequestException("studentId is required when role is STUDENT");
        }

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + request.studentId()));

        userRepository.findByStudent_Id(request.studentId())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Student with id " + request.studentId() + " is already linked to a user account");
                });

        return student;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStudent() == null ? null : user.getStudent().getId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
