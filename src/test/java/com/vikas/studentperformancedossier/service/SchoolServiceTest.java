package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.SchoolRequest;
import com.vikas.studentperformancedossier.dto.SchoolResponse;
import com.vikas.studentperformancedossier.entity.School;
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
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolService schoolService;

    private SchoolRequest request;

    @BeforeEach
    void setUp() {
        request = new SchoolRequest("Central High", "123 Main St");
    }

    @Test
    void create_savesSchool() {
        when(schoolRepository.save(any(School.class))).thenReturn(existingSchool(1L));

        SchoolResponse response = schoolService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Central High");
        assertThat(response.address()).isEqualTo("123 Main St");
    }

    @Test
    void update_whenSchoolNotFound_throwsEntityNotFoundException() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(schoolRepository, never()).save(any());
    }

    @Test
    void update_whenSchoolExists_updatesSuccessfully() {
        School existing = existingSchool(1L);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(schoolRepository.save(any(School.class))).thenReturn(existing);

        SchoolResponse response = schoolService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(schoolRepository).save(existing);
    }

    @Test
    void findById_whenSchoolNotFound_throwsEntityNotFoundException() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void delete_whenSchoolNotFound_throwsEntityNotFoundException() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(schoolRepository, never()).delete(any());
    }

    @Test
    void delete_whenSchoolExists_deletesSchool() {
        School existing = existingSchool(1L);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(existing));

        schoolService.delete(1L);

        verify(schoolRepository).delete(existing);
    }

    private School existingSchool(Long id) {
        School school = new School();
        school.setId(id);
        school.setName("Central High");
        school.setAddress("123 Main St");
        return school;
    }
}
