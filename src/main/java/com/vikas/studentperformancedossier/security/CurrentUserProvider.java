package com.vikas.studentperformancedossier.security;

import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Re-looks up the domain User by the authenticated username rather than casting the security
// principal, so this works the same whether the principal came from real JWT auth or a test's
// @WithMockUser (both implement UserDetails, so Authentication.getName() resolves the username).
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }
}
