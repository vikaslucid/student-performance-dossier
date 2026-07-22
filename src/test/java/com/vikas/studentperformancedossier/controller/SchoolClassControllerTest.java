package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.SchoolClassRequest;
import com.vikas.studentperformancedossier.dto.SchoolClassResponse;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.service.SchoolClassService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchoolClassController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class SchoolClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SchoolClassService schoolClassService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllSchoolClasses_returnsList() throws Exception {
        when(schoolClassService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/school-classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].grade").value("Grade 10"));
    }

    @Test
    void getAllSchoolClasses_whenEmpty_returnsEmptyList() throws Exception {
        when(schoolClassService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/school-classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSchoolClassById_whenFound_returnsSchoolClass() throws Exception {
        when(schoolClassService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/school-classes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.section").value("A"));
    }

    @Test
    void getSchoolClassById_whenMissing_returns404() throws Exception {
        when(schoolClassService.findById(99L))
                .thenThrow(new EntityNotFoundException("School class not found with id: 99"));

        mockMvc.perform(get("/api/school-classes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School class not found with id: 99"));
    }

    @Test
    void createSchoolClass_whenValid_returns201() throws Exception {
        SchoolClassRequest request = sampleRequest();
        when(schoolClassService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/school-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.grade").value("Grade 10"));
    }

    @Test
    void createSchoolClass_whenGradeBlank_returns400() throws Exception {
        SchoolClassRequest invalidRequest = new SchoolClassRequest("", null, "A", 1L);

        mockMvc.perform(post("/api/school-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.grade").exists());
    }

    @Test
    void createSchoolClass_whenSectionBlank_returns400() throws Exception {
        SchoolClassRequest invalidRequest = new SchoolClassRequest("Grade 10", null, "", 1L);

        mockMvc.perform(post("/api/school-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.section").exists());
    }

    @Test
    void createSchoolClass_whenSchoolIdMissing_returns400() throws Exception {
        SchoolClassRequest invalidRequest = new SchoolClassRequest("Grade 10", null, "A", null);

        mockMvc.perform(post("/api/school-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolId").exists());
    }

    @Test
    void createSchoolClass_whenSchoolNotFound_returns404() throws Exception {
        SchoolClassRequest request = sampleRequest();
        when(schoolClassService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("School not found with id: 1"));

        mockMvc.perform(post("/api/school-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School not found with id: 1"));
    }

    @Test
    void createSchoolClass_whenDuplicate_returns409() throws Exception {
        SchoolClassRequest request = sampleRequest();
        when(schoolClassService.create(eq(request)))
                .thenThrow(new DuplicateResourceException(
                        "A school class with grade 'Grade 10' and section 'A' already exists for school id 1"));

        mockMvc.perform(post("/api/school-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "A school class with grade 'Grade 10' and section 'A' already exists for school id 1"));
    }

    @Test
    void updateSchoolClass_whenValid_returns200() throws Exception {
        SchoolClassRequest request = sampleRequest();
        when(schoolClassService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/school-classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateSchoolClass_whenMissing_returns404() throws Exception {
        SchoolClassRequest request = sampleRequest();
        when(schoolClassService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("School class not found with id: 99"));

        mockMvc.perform(put("/api/school-classes/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School class not found with id: 99"));
    }

    @Test
    void updateSchoolClass_whenDuplicate_returns409() throws Exception {
        SchoolClassRequest request = sampleRequest();
        when(schoolClassService.update(eq(1L), eq(request)))
                .thenThrow(new DuplicateResourceException(
                        "A school class with grade 'Grade 10' and section 'A' already exists for school id 1"));

        mockMvc.perform(put("/api/school-classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSchoolClass_whenInvalid_returns400WithFieldErrors() throws Exception {
        SchoolClassRequest invalidRequest = new SchoolClassRequest("", null, "", null);

        mockMvc.perform(put("/api/school-classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.grade").exists())
                .andExpect(jsonPath("$.errors.section").exists())
                .andExpect(jsonPath("$.errors.schoolId").exists());
    }

    @Test
    void deleteSchoolClass_returns204() throws Exception {
        mockMvc.perform(delete("/api/school-classes/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSchoolClass_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("School class not found with id: 99"))
                .when(schoolClassService).delete(99L);

        mockMvc.perform(delete("/api/school-classes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School class not found with id: 99"));
    }

    private SchoolClassRequest sampleRequest() {
        return new SchoolClassRequest("Grade 10", null, "A", 1L);
    }

    private SchoolClassResponse sampleResponse(Long id) {
        return new SchoolClassResponse(
                id,
                "Grade 10",
                null,
                "A",
                1L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
