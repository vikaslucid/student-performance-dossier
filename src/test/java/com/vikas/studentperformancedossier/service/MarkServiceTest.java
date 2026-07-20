package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.MarkRequest;
import com.vikas.studentperformancedossier.dto.MarkResponse;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.Mark;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.ExamRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkServiceTest {

    @Mock
    private MarkRepository markRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private MarkService markService;

    private MarkRequest request;

    @BeforeEach
    void setUp() {
        request = new MarkRequest(85, 100, "A", "Well done", 1L, 2L);
    }

    @Test
    void create_whenMarkAlreadyExistsForStudentAndExam_throwsDuplicateResourceException() {
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L))
                .thenReturn(Optional.of(existingMark(3L)));

        assertThatThrownBy(() -> markService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("2");

        verify(markRepository, never()).save(any());
    }

    @Test
    void create_whenStudentNotFound_throwsEntityNotFoundException() {
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(markRepository, never()).save(any());
    }

    @Test
    void create_whenExamNotFound_throwsEntityNotFoundException() {
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent(1L)));
        when(examRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("2");

        verify(markRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicatesAndReferencesExist_savesMark() {
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent(1L)));
        when(examRepository.findById(2L)).thenReturn(Optional.of(existingExam(2L)));
        when(markRepository.save(any(Mark.class))).thenReturn(existingMark(1L));

        MarkResponse response = markService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.obtainedMarks()).isEqualTo(85);
        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.examId()).isEqualTo(2L);
    }

    @Test
    void update_whenMarkBelongsToAnotherRecord_throwsDuplicateResourceException() {
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L))
                .thenReturn(Optional.of(existingMark(3L)));

        assertThatThrownBy(() -> markService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(markRepository, never()).findById(any());
        verify(markRepository, never()).save(any());
    }

    @Test
    void update_whenMarkNotFound_throwsEntityNotFoundException() {
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L)).thenReturn(Optional.empty());
        when(markRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(markRepository, never()).save(any());
    }

    @Test
    void update_whenMarkBelongsToSameRecord_updatesSuccessfully() {
        Mark self = existingMark(1L);
        when(markRepository.findByStudent_IdAndExam_Id(1L, 2L)).thenReturn(Optional.of(self));
        when(markRepository.findById(1L)).thenReturn(Optional.of(self));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent(1L)));
        when(examRepository.findById(2L)).thenReturn(Optional.of(existingExam(2L)));
        when(markRepository.save(any(Mark.class))).thenReturn(self);

        MarkResponse response = markService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(markRepository).save(self);
    }

    @Test
    void delete_whenMarkNotFound_throwsEntityNotFoundException() {
        when(markRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(markRepository, never()).delete(any());
    }

    @Test
    void delete_whenMarkExists_deletesMark() {
        Mark existing = existingMark(1L);
        when(markRepository.findById(1L)).thenReturn(Optional.of(existing));

        markService.delete(1L);

        verify(markRepository).delete(existing);
    }

    @Test
    void findAll_whenAdmin_returnsAllMarks() {
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(markRepository.findAll()).thenReturn(List.of(existingMark(1L)));

        List<MarkResponse> responses = markService.findAll();

        assertThat(responses).hasSize(1);
        verify(markRepository, never()).findByStudent_Id(any());
    }

    @Test
    void findAll_whenTeacher_returnsAllMarks() {
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.TEACHER, null));
        when(markRepository.findAll()).thenReturn(List.of(existingMark(1L)));

        List<MarkResponse> responses = markService.findAll();

        assertThat(responses).hasSize(1);
        verify(markRepository, never()).findByStudent_Id(any());
    }

    @Test
    void findAll_whenStudent_returnsOnlyOwnMarks() {
        Student linkedStudent = existingStudent(1L);
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, linkedStudent));
        when(markRepository.findByStudent_Id(1L)).thenReturn(List.of(existingMark(1L)));

        List<MarkResponse> responses = markService.findAll();

        assertThat(responses).hasSize(1);
        verify(markRepository, never()).findAll();
    }

    @Test
    void findAll_whenStudentHasNoLinkedStudent_returnsEmptyList() {
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, null));

        List<MarkResponse> responses = markService.findAll();

        assertThat(responses).isEmpty();
        verify(markRepository, never()).findAll();
        verify(markRepository, never()).findByStudent_Id(any());
    }

    @Test
    void findById_whenStudentOwnsMark_returnsMark() {
        Student linkedStudent = existingStudent(1L);
        when(markRepository.findById(1L)).thenReturn(Optional.of(existingMark(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, linkedStudent));

        MarkResponse response = markService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findById_whenStudentDoesNotOwnMark_throwsEntityNotFoundException() {
        Student otherStudent = existingStudent(99L);
        when(markRepository.findById(1L)).thenReturn(Optional.of(existingMark(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, otherStudent));

        assertThatThrownBy(() -> markService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void findById_whenStudentHasNoLinkedStudent_throwsEntityNotFoundException() {
        when(markRepository.findById(1L)).thenReturn(Optional.of(existingMark(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, null));

        assertThatThrownBy(() -> markService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findById_whenAdmin_returnsAnyMark() {
        when(markRepository.findById(1L)).thenReturn(Optional.of(existingMark(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));

        MarkResponse response = markService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findById_whenTeacher_returnsAnyMark() {
        when(markRepository.findById(1L)).thenReturn(Optional.of(existingMark(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.TEACHER, null));

        MarkResponse response = markService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
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

    private Mark existingMark(Long id) {
        Mark mark = new Mark();
        mark.setId(id);
        mark.setObtainedMarks(85);
        mark.setMaximumMarks(100);
        mark.setGrade("A");
        mark.setRemarks("Well done");
        mark.setStudent(existingStudent(1L));
        mark.setExam(existingExam(2L));
        return mark;
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

    private Exam existingExam(Long id) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setName("Midterm");
        exam.setExamDate(LocalDate.of(2026, 3, 1));
        return exam;
    }
}
