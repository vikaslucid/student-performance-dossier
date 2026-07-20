package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_whenExists_returnsUser() {
        User user = persistedUser("ada", Role.ADMIN);

        Optional<User> found = userRepository.findByUsername("ada");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByUsername_whenMissing_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("missing");

        assertThat(found).isEmpty();
    }

    private User persistedUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setRole(role);
        return entityManager.persistFlushFind(user);
    }
}
