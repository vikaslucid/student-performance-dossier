package com.vikas.studentperformancedossier.security;

import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserProvider currentUserProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_returnsUserMatchingAuthenticatedUsername() {
        authenticateAs("ada");
        User user = existingUser(1L, "ada");
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(user));

        User result = currentUserProvider.getCurrentUser();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("ada");
    }

    @Test
    void getCurrentUser_whenUserNotFound_throwsEntityNotFoundException() {
        authenticateAs("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserProvider.getCurrentUser())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private User existingUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setRole(Role.STUDENT);
        return user;
    }
}
