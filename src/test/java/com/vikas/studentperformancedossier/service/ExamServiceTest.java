package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.ExamRequest;
import com.vikas.studentperformancedossier.dto.ExamResponse;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.ExamRepository;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import com.vikas.studentperformancedossier.repository.SubjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private ExamService examService;

    private ExamRequest request;

    @BeforeEach
    void setUp() {
        request = new ExamRequest("Midterm", LocalDate.of(2026, 3, 1), 1L, 2L);
    }

    @Test
    void create_whenNameAlreadyExistsForSchoolClassAndSubject_throwsDuplicateResourceException() {
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.of(existingExam(3L, "Midterm")));

        assertThatThrownBy(() -> examService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Midterm");

        verify(examRepository, never()).save(any());
    }

    @Test
    void create_whenSchoolClassNotFound_throwsEntityNotFoundException() {
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(examRepository, never()).save(any());
    }

    @Test
    void create_whenSubjectNotFound_throwsEntityNotFoundException() {
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existingSchoolClass(1L)));
        when(subjectRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("2");

        verify(examRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicatesAndReferencesExist_savesExam() {
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existingSchoolClass(1L)));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(existingSubject(2L)));
        when(examRepository.save(any(Exam.class))).thenReturn(existingExam(1L, "Midterm"));

        ExamResponse response = examService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Midterm");
        assertThat(response.schoolClassId()).isEqualTo(1L);
        assertThat(response.subjectId()).isEqualTo(2L);
    }

    @Test
    void update_whenNameBelongsToAnotherExam_throwsDuplicateResourceException() {
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.of(existingExam(3L, "Midterm")));

        assertThatThrownBy(() -> examService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(examRepository, never()).findById(any());
        verify(examRepository, never()).save(any());
    }

    @Test
    void update_whenExamNotFound_throwsEntityNotFoundException() {
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.empty());
        when(examRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(examRepository, never()).save(any());
    }

    @Test
    void update_whenNameBelongsToSameExam_updatesSuccessfully() {
        Exam self = existingExam(1L, "Midterm");
        when(examRepository.findBySchoolClass_IdAndSubject_IdAndName(1L, 2L, "Midterm"))
                .thenReturn(Optional.of(self));
        when(examRepository.findById(1L)).thenReturn(Optional.of(self));
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existingSchoolClass(1L)));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(existingSubject(2L)));
        when(examRepository.save(any(Exam.class))).thenReturn(self);

        ExamResponse response = examService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(examRepository).save(self);
    }

    @Test
    void delete_whenExamNotFound_throwsEntityNotFoundException() {
        when(examRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(examRepository, never()).delete(any());
    }

    @Test
    void delete_whenExamExists_deletesExam() {
        Exam existing = existingExam(1L, "Midterm");
        when(examRepository.findById(1L)).thenReturn(Optional.of(existing));

        examService.delete(1L);

        verify(examRepository).delete(existing);
    }

    private Exam existingExam(Long id, String name) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setName(name);
        exam.setExamDate(LocalDate.of(2026, 3, 1));
        exam.setSchoolClass(existingSchoolClass(1L));
        exam.setSubject(existingSubject(2L));
        return exam;
    }

    private SchoolClass existingSchoolClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade("Grade 10");
        schoolClass.setSection("A");
        return schoolClass;
    }

    private Subject existingSubject(Long id) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Mathematics");
        subject.setGradeLevel("Grade 10");
        return subject;
    }
}
