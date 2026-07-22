package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.SchoolClassRequest;
import com.vikas.studentperformancedossier.dto.SchoolClassResponse;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import com.vikas.studentperformancedossier.repository.SchoolRepository;
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
class SchoolClassServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolClassService schoolClassService;

    private SchoolClassRequest request;

    @BeforeEach
    void setUp() {
        request = new SchoolClassRequest("Grade 10", null, "A", 1L);
    }

    @Test
    void create_whenGradeAndSectionAlreadyExistForSchool_throwsDuplicateResourceException() {
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Grade 10", null, "A"))
                .thenReturn(Optional.of(existingSchoolClass(2L, "Grade 10", null, "A")));

        assertThatThrownBy(() -> schoolClassService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Grade 10")
                .hasMessageContaining("A");

        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void create_whenSchoolNotFound_throwsEntityNotFoundException() {
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Grade 10", null, "A"))
                .thenReturn(Optional.empty());
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolClassService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicatesAndSchoolExists_savesSchoolClass() {
        School school = existingSchool(1L);
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Grade 10", null, "A"))
                .thenReturn(Optional.empty());
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolClassRepository.save(any(SchoolClass.class)))
                .thenReturn(existingSchoolClass(1L, "Grade 10", null, "A"));

        SchoolClassResponse response = schoolClassService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.grade()).isEqualTo("Grade 10");
        assertThat(response.section()).isEqualTo("A");
    }

    @Test
    void create_withStream_savesSchoolClass() {
        SchoolClassRequest requestWithStream = new SchoolClassRequest("Eleventh", "ARTS", "A", 1L);
        School school = existingSchool(1L);
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Eleventh", "ARTS", "A"))
                .thenReturn(Optional.empty());
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolClassRepository.save(any(SchoolClass.class)))
                .thenReturn(existingSchoolClass(1L, "Eleventh", "ARTS", "A"));

        SchoolClassResponse response = schoolClassService.create(requestWithStream);

        assertThat(response.stream()).isEqualTo("ARTS");
    }

    @Test
    void update_whenGradeAndSectionBelongToAnotherSchoolClass_throwsDuplicateResourceException() {
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Grade 10", null, "A"))
                .thenReturn(Optional.of(existingSchoolClass(2L, "Grade 10", null, "A")));

        assertThatThrownBy(() -> schoolClassService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(schoolClassRepository, never()).findById(any());
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void update_whenSchoolClassNotFound_throwsEntityNotFoundException() {
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Grade 10", null, "A"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolClassService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void update_whenGradeAndSectionBelongToSameSchoolClass_updatesSuccessfully() {
        SchoolClass self = existingSchoolClass(1L, "Grade 10", null, "A");
        School school = existingSchool(1L);
        when(schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(1L, "Grade 10", null, "A"))
                .thenReturn(Optional.of(self));
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(self));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenReturn(self);

        SchoolClassResponse response = schoolClassService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(schoolClassRepository).save(self);
    }

    @Test
    void delete_whenSchoolClassNotFound_throwsEntityNotFoundException() {
        when(schoolClassRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolClassService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(schoolClassRepository, never()).delete(any());
    }

    @Test
    void delete_whenSchoolClassExists_deletesSchoolClass() {
        SchoolClass existing = existingSchoolClass(1L, "Grade 10", null, "A");
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existing));

        schoolClassService.delete(1L);

        verify(schoolClassRepository).delete(existing);
    }

    private SchoolClass existingSchoolClass(Long id, String grade, String stream, String section) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade(grade);
        schoolClass.setStream(stream);
        schoolClass.setSection(section);
        schoolClass.setSchool(existingSchool(1L));
        return schoolClass;
    }

    private School existingSchool(Long id) {
        School school = new School();
        school.setId(id);
        school.setName("Central High");
        school.setAddress("123 Main St");
        return school;
    }
}
