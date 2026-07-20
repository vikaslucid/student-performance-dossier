package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Subject;
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
class ExamRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExamRepository examRepository;

    @Test
    void findBySchoolClassIdAndSubjectIdAndName_whenExists_returnsExam() {
        SchoolClass schoolClass = persistedSchoolClass();
        Subject subject = persistedSubject();
        Exam exam = persistedExam(schoolClass, subject, "Midterm");

        Optional<Exam> found = examRepository.findBySchoolClass_IdAndSubject_IdAndName(
                schoolClass.getId(), subject.getId(), "Midterm");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(exam.getId());
    }

    @Test
    void findBySchoolClassIdAndSubjectIdAndName_whenMissing_returnsEmpty() {
        SchoolClass schoolClass = persistedSchoolClass();
        Subject subject = persistedSubject();

        Optional<Exam> found = examRepository.findBySchoolClass_IdAndSubject_IdAndName(
                schoolClass.getId(), subject.getId(), "Final");

        assertThat(found).isEmpty();
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

    private Subject persistedSubject() {
        Subject subject = new Subject();
        subject.setName("Mathematics");
        subject.setGradeLevel("Grade 10");
        return entityManager.persistFlushFind(subject);
    }

    private Exam persistedExam(SchoolClass schoolClass, Subject subject, String name) {
        Exam exam = new Exam();
        exam.setName(name);
        exam.setExamDate(LocalDate.of(2026, 3, 1));
        exam.setSchoolClass(schoolClass);
        exam.setSubject(subject);
        return entityManager.persistFlushFind(exam);
    }
}
