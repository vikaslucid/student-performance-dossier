package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.Mark;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.Subject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class MarkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MarkRepository markRepository;

    @Test
    void findByStudentIdAndExamId_whenExists_returnsMark() {
        Student student = persistedStudent();
        Exam exam = persistedExam();
        Mark mark = persistedMark(student, exam);

        Optional<Mark> found = markRepository.findByStudent_IdAndExam_Id(student.getId(), exam.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(mark.getId());
    }

    @Test
    void findByStudentIdAndExamId_whenMissing_returnsEmpty() {
        Student student = persistedStudent();
        Exam exam = persistedExam();

        Optional<Mark> found = markRepository.findByStudent_IdAndExam_Id(student.getId(), exam.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByStudentId_returnsOnlyMarksForThatStudent() {
        Student ada = persistedStudent("ada@example.com", "S-100");
        Student grace = persistedStudent("grace@example.com", "S-200");
        Exam exam = persistedExam();
        Mark adaMark = persistedMark(ada, exam);
        persistedMark(grace, exam);

        List<Mark> found = markRepository.findByStudent_Id(ada.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(adaMark.getId());
    }

    @Test
    void findByStudentId_whenStudentHasNoMarks_returnsEmptyList() {
        Student student = persistedStudent();

        List<Mark> found = markRepository.findByStudent_Id(student.getId());

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

    private Student persistedStudent() {
        return persistedStudent("ada@example.com", "S-100");
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

    private Exam persistedExam() {
        Subject subject = new Subject();
        subject.setName("Mathematics");
        subject.setGradeLevel("Grade 10");
        entityManager.persistAndFlush(subject);

        Exam exam = new Exam();
        exam.setName("Midterm");
        exam.setExamDate(LocalDate.of(2026, 3, 1));
        exam.setSchoolClass(persistedSchoolClass());
        exam.setSubject(subject);
        return entityManager.persistFlushFind(exam);
    }

    private Mark persistedMark(Student student, Exam exam) {
        Mark mark = new Mark();
        mark.setConcept(4);
        mark.setApplication(4);
        mark.setAccuracy(4);
        mark.setHomework(4);
        mark.setTest(4);
        mark.setRemarks("Well done");
        mark.setStudent(student);
        mark.setExam(exam);
        return entityManager.persistFlushFind(mark);
    }
}
