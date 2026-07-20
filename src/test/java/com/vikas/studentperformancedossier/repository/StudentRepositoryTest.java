package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class StudentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void findByEmail_whenExists_returnsStudent() {
        Student student = persistedStudent("ada@example.com", "S-100");

        Optional<Student> found = studentRepository.findByEmail("ada@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(student.getId());
    }

    @Test
    void findByEmail_whenMissing_returnsEmpty() {
        Optional<Student> found = studentRepository.findByEmail("missing@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findByStudentNumber_whenExists_returnsStudent() {
        Student student = persistedStudent("grace@example.com", "S-200");

        Optional<Student> found = studentRepository.findByStudentNumber("S-200");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(student.getId());
    }

    @Test
    void findByStudentNumber_whenMissing_returnsEmpty() {
        Optional<Student> found = studentRepository.findByStudentNumber("S-999");

        assertThat(found).isEmpty();
    }

    private Student persistedStudent(String email, String studentNumber) {
        Student student = new Student();
        student.setFirstName("Ada");
        student.setLastName("Lovelace");
        student.setEmail(email);
        student.setDateOfBirth(LocalDate.of(1990, 1, 1));
        student.setEnrollmentDate(LocalDate.of(2020, 1, 1));
        student.setStudentNumber(studentNumber);
        student.setSchoolClass(persistedSchoolClass());
        return entityManager.persistFlushFind(student);
    }

    private SchoolClass persistedSchoolClass() {
        School school = new School();
        school.setName("Central High");
        school.setAddress("123 Main St");
        entityManager.persistAndFlush(school);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setGrade("Grade 10");
        schoolClass.setSection("A");
        schoolClass.setSchool(school);
        return entityManager.persistFlushFind(schoolClass);
    }
}
