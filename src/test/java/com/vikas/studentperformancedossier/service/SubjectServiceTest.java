package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.SubjectRequest;
import com.vikas.studentperformancedossier.dto.SubjectResponse;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.SubjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SubjectService subjectService;

    private SubjectRequest request;

    @BeforeEach
    void setUp() {
        request = new SubjectRequest("Mathematics", "Grade 10");
    }

    @Test
    void create_whenNameAndGradeLevelAlreadyExist_throwsDuplicateResourceException() {
        when(subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 10"))
                .thenReturn(Optional.of(existingSubject(2L, "Mathematics", "Grade 10")));

        assertThatThrownBy(() -> subjectService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Mathematics")
                .hasMessageContaining("Grade 10");

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicates_savesSubject() {
        when(subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 10")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class)))
                .thenReturn(existingSubject(1L, "Mathematics", "Grade 10"));

        SubjectResponse response = subjectService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Mathematics");
        assertThat(response.gradeLevel()).isEqualTo("Grade 10");
    }

    @Test
    void update_whenNameAndGradeLevelBelongToAnotherSubject_throwsDuplicateResourceException() {
        when(subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 10"))
                .thenReturn(Optional.of(existingSubject(2L, "Mathematics", "Grade 10")));

        assertThatThrownBy(() -> subjectService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(subjectRepository, never()).findById(any());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void update_whenNameAndGradeLevelBelongToSameSubject_updatesSuccessfully() {
        Subject self = existingSubject(1L, "Mathematics", "Grade 10");
        when(subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 10")).thenReturn(Optional.of(self));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(self));
        when(subjectRepository.save(any(Subject.class))).thenReturn(self);

        SubjectResponse response = subjectService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(subjectRepository).save(self);
    }

    @Test
    void update_whenSubjectNotFound_throwsEntityNotFoundException() {
        when(subjectRepository.findByNameAndGradeLevel("Mathematics", "Grade 10")).thenReturn(Optional.empty());
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void delete_whenSubjectNotFound_throwsEntityNotFoundException() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void delete_whenSubjectExists_deletesSubject() {
        Subject existing = existingSubject(1L, "Mathematics", "Grade 10");
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existing));

        subjectService.delete(1L);

        verify(subjectRepository).delete(existing);
    }

    private Subject existingSubject(Long id, String name, String gradeLevel) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        subject.setGradeLevel(gradeLevel);
        return subject;
    }
}
