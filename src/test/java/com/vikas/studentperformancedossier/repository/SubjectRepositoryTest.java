package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.Subject;
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
class SubjectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubjectRepository subjectRepository;

    @Test
    void findByNameAndGradeLevel_whenExists_returnsSubject() {
        Subject subject = persistedSubject("Mathematics", "Grade 10");

        Optional<Subject> found = subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 10");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(subject.getId());
    }

    @Test
    void findByNameAndGradeLevel_whenMissing_returnsEmpty() {
        Optional<Subject> found = subjectRepository.findByNameAndGradeLevel("Missing Subject", "Grade 10");

        assertThat(found).isEmpty();
    }

    @Test
    void findByNameAndGradeLevel_whenSameNameDifferentGradeLevel_returnsEmpty() {
        persistedSubject("Mathematics", "Grade 10");

        Optional<Subject> found = subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 11");

        assertThat(found).isEmpty();
    }

    private Subject persistedSubject(String name, String gradeLevel) {
        Subject subject = new Subject();
        subject.setName(name);
        subject.setGradeLevel(gradeLevel);
        return entityManager.persistFlushFind(subject);
    }
}
