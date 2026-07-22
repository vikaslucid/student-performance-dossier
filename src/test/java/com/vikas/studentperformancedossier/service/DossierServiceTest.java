package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import com.vikas.studentperformancedossier.entity.Behaviour;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.Mark;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.repository.BehaviourRepository;
import com.vikas.studentperformancedossier.repository.MarkRepository;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import com.vikas.studentperformancedossier.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {

    @Mock
    private MarkRepository markRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BehaviourRepository behaviourRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private DossierService dossierService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = existingStudent(1L);
    }

    @Test
    void getDossier_whenStudentNotFound_throwsEntityNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.getDossier(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getDossier_whenAdmin_returnsDossierForAnyStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of());

        DossierResponse response = dossierService.getDossier(1L);

        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.studentName()).isEqualTo("Ada Lovelace");
        assertThat(response.studentNumber()).isEqualTo("S-100");
        assertThat(response.schoolName()).isEqualTo("Central High");
        assertThat(response.grade()).isEqualTo("Grade 10");
        assertThat(response.section()).isEqualTo("A");
    }

    @Test
    void getDossier_whenTeacher_returnsDossierForAnyStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.TEACHER, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of());

        DossierResponse response = dossierService.getDossier(1L);

        assertThat(response.studentId()).isEqualTo(1L);
    }

    @Test
    void getDossier_whenStudentRequestsOwnRecord_returnsDossier() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, student));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of());

        DossierResponse response = dossierService.getDossier(1L);

        assertThat(response.studentId()).isEqualTo(1L);
    }

    @Test
    void getDossier_whenStudentRequestsAnotherStudentsRecord_throwsEntityNotFoundException() {
        Student otherStudent = existingStudent(2L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, otherStudent));

        assertThatThrownBy(() -> dossierService.getDossier(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void getDossier_whenStudentHasNoLinkedStudent_throwsEntityNotFoundException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, null));

        assertThatThrownBy(() -> dossierService.getDossier(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getDossier_whenNoMarks_returnsNullAveragesAndEmptyLists() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of());

        DossierResponse response = dossierService.getDossier(1L);

        assertThat(response.overallAveragePercentage()).isNull();
        assertThat(response.overallGrade()).isNull();
        assertThat(response.subjectAverages()).isEmpty();
        assertThat(response.examSummaries()).isEmpty();
        assertThat(response.behaviour()).isNull();
    }

    @Test
    void getDossier_whenBehaviourRecordExists_includesItInResponse() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of());
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existingBehaviour()));

        DossierResponse response = dossierService.getDossier(1L);

        assertThat(response.behaviour()).isNotNull();
        assertThat(response.behaviour().overallBehaviour()).isEqualTo(4);
    }

    @Test
    void getDossier_computesOverallAverageAsMeanOfPercentages() {
        Subject math = existingSubject(1L, "Mathematics");
        Exam exam1 = existingExam(1L, "Midterm", math, LocalDate.of(2026, 2, 1));
        Exam exam2 = existingExam(2L, "Final", math, LocalDate.of(2026, 5, 1));
        // Every mark's total is out of a fixed 25 (5 components x 0-5), so percentage is always
        // total * 4 - total=20 -> 80%, total=16 -> 64%, mean = 72%.
        Mark mark1 = existingMark(1L, 20, exam1);
        Mark mark2 = existingMark(2L, 16, exam2);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of(mark1, mark2));

        DossierResponse response = dossierService.getDossier(1L);

        assertThat(response.overallAveragePercentage()).isEqualTo(72.0);
        assertThat(response.overallGrade()).isEqualTo("B");
    }

    @Test
    void getDossier_computesSubjectWiseAverages() {
        Subject math = existingSubject(1L, "Mathematics");
        Subject science = existingSubject(2L, "Science");
        Exam mathExam1 = existingExam(1L, "Midterm", math, LocalDate.of(2026, 2, 1));
        Exam mathExam2 = existingExam(2L, "Final", math, LocalDate.of(2026, 5, 1));
        Exam scienceExam = existingExam(3L, "Midterm", science, LocalDate.of(2026, 2, 1));

        Mark mathMark1 = existingMark(1L, 20, mathExam1);
        Mark mathMark2 = existingMark(2L, 15, mathExam2);
        Mark scienceMark = existingMark(3L, 13, scienceExam);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of(mathMark1, mathMark2, scienceMark));

        DossierResponse response = dossierService.getDossier(1L);

        List<SubjectAverageResponse> subjectAverages = response.subjectAverages();
        assertThat(subjectAverages).hasSize(2);
        SubjectAverageResponse mathAverage = subjectAverages.stream()
                .filter(s -> s.subjectId().equals(1L)).findFirst().orElseThrow();
        SubjectAverageResponse scienceAverage = subjectAverages.stream()
                .filter(s -> s.subjectId().equals(2L)).findFirst().orElseThrow();
        assertThat(mathAverage.averagePercentage()).isEqualTo(70.0);
        assertThat(scienceAverage.averagePercentage()).isEqualTo(52.0);
    }

    @Test
    void getDossier_marksExamAsPassedOrFailedBasedOnFortyPercentThreshold() {
        Subject math = existingSubject(1L, "Mathematics");
        Exam passingExam = existingExam(1L, "Midterm", math, LocalDate.of(2026, 2, 1));
        Exam failingExam = existingExam(2L, "Final", math, LocalDate.of(2026, 5, 1));
        Mark passingMark = existingMark(1L, 13, passingExam); // 52% -> grade C
        Mark failingMark = existingMark(2L, 9, failingExam); // 36% -> grade D

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of(passingMark, failingMark));

        DossierResponse response = dossierService.getDossier(1L);

        ExamSummaryResponse passingSummary = response.examSummaries().stream()
                .filter(e -> e.examId().equals(1L)).findFirst().orElseThrow();
        ExamSummaryResponse failingSummary = response.examSummaries().stream()
                .filter(e -> e.examId().equals(2L)).findFirst().orElseThrow();

        assertThat(passingSummary.passed()).isTrue();
        assertThat(passingSummary.grade()).isEqualTo("C");
        assertThat(failingSummary.passed()).isFalse();
        assertThat(failingSummary.grade()).isEqualTo("D");
    }

    private User existingUser(Role role, Student linkedStudent) {
        User user = new User();
        user.setId(1L);
        user.setUsername("ada");
        user.setPassword("hashed-password");
        user.setRole(role);
        user.setStudent(linkedStudent);
        return user;
    }

    private Student existingStudent(Long id) {
        Student s = new Student();
        s.setId(id);
        s.setFirstName("Ada");
        s.setLastName("Lovelace");
        s.setEmail("ada" + id + "@example.com");
        s.setDateOfBirth(LocalDate.of(1990, 1, 1));
        s.setEnrollmentDate(LocalDate.of(2020, 1, 1));
        s.setStudentNumber("S-100");
        s.setSchoolClass(existingSchoolClass());
        return s;
    }

    private SchoolClass existingSchoolClass() {
        School school = new School();
        school.setId(1L);
        school.setName("Central High");
        school.setAddress("123 Main St");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(1L);
        schoolClass.setGrade("Grade 10");
        schoolClass.setSection("A");
        schoolClass.setSchool(school);
        return schoolClass;
    }

    private Subject existingSubject(Long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        subject.setGradeLevel("Grade 10");
        return subject;
    }

    private Exam existingExam(Long id, String name, Subject subject, LocalDate examDate) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setName(name);
        exam.setExamDate(examDate);
        exam.setSubject(subject);
        exam.setSchoolClass(new SchoolClass());
        return exam;
    }

    private Behaviour existingBehaviour() {
        Behaviour behaviour = new Behaviour();
        behaviour.setId(1L);
        behaviour.setAttention(4);
        behaviour.setParticipation(4);
        behaviour.setDiscipline(4);
        behaviour.setHomeworkResponsibility(4);
        behaviour.setCommunicationSkills(4);
        behaviour.setConfidence(4);
        behaviour.setTeamwork(4);
        behaviour.setCuriosity(4);
        behaviour.setLeadership(4);
        behaviour.setCriticalThinking(4);
        behaviour.setOverallBehaviour(4);
        behaviour.setStudent(student);
        return behaviour;
    }

    // Distributes the given total (0-25) across the 5 components (each capped at 0-5) - the exact
    // split doesn't matter for these tests, only the resulting total/percentage/grade.
    private Mark existingMark(Long id, int total, Exam exam) {
        Mark mark = new Mark();
        mark.setId(id);
        int remaining = total;
        int[] components = new int[5];
        for (int i = 0; i < 5; i++) {
            int share = Math.min(5, remaining);
            components[i] = share;
            remaining -= share;
        }
        mark.setConcept(components[0]);
        mark.setApplication(components[1]);
        mark.setAccuracy(components[2]);
        mark.setHomework(components[3]);
        mark.setTest(components[4]);
        mark.setStudent(student);
        mark.setExam(exam);
        return mark;
    }
}
