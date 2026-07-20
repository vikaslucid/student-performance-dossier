package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.dto.SubjectRequest;
import com.vikas.studentperformancedossier.dto.SubjectResponse;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.service.SubjectService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubjectService subjectService;

    @Test
    void getAllSubjects_returnsList() throws Exception {
        when(subjectService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Mathematics"));
    }

    @Test
    void getAllSubjects_whenEmpty_returnsEmptyList() throws Exception {
        when(subjectService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSubjectById_whenFound_returnsSubject() throws Exception {
        when(subjectService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/subjects/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.gradeLevel").value("Grade 10"));
    }

    @Test
    void getSubjectById_whenMissing_returns404() throws Exception {
        when(subjectService.findById(99L))
                .thenThrow(new EntityNotFoundException("Subject not found with id: 99"));

        mockMvc.perform(get("/api/subjects/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Subject not found with id: 99"));
    }

    @Test
    void createSubject_whenValid_returns201() throws Exception {
        SubjectRequest request = sampleRequest();
        when(subjectService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mathematics"));
    }

    @Test
    void createSubject_whenNameBlank_returns400() throws Exception {
        SubjectRequest invalidRequest = new SubjectRequest("", "Grade 10");

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createSubject_whenGradeLevelBlank_returns400() throws Exception {
        SubjectRequest invalidRequest = new SubjectRequest("Mathematics", "");

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.gradeLevel").exists());
    }

    @Test
    void createSubject_whenDuplicate_returns409() throws Exception {
        SubjectRequest request = sampleRequest();
        when(subjectService.create(eq(request)))
                .thenThrow(new DuplicateResourceException(
                        "A subject named 'Mathematics' already exists for grade level 'Grade 10'"));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "A subject named 'Mathematics' already exists for grade level 'Grade 10'"));
    }

    @Test
    void updateSubject_whenValid_returns200() throws Exception {
        SubjectRequest request = sampleRequest();
        when(subjectService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/subjects/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateSubject_whenMissing_returns404() throws Exception {
        SubjectRequest request = sampleRequest();
        when(subjectService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("Subject not found with id: 99"));

        mockMvc.perform(put("/api/subjects/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Subject not found with id: 99"));
    }

    @Test
    void updateSubject_whenDuplicate_returns409() throws Exception {
        SubjectRequest request = sampleRequest();
        when(subjectService.update(eq(1L), eq(request)))
                .thenThrow(new DuplicateResourceException(
                        "A subject named 'Mathematics' already exists for grade level 'Grade 10'"));

        mockMvc.perform(put("/api/subjects/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSubject_whenInvalid_returns400WithFieldErrors() throws Exception {
        SubjectRequest invalidRequest = new SubjectRequest("", "");

        mockMvc.perform(put("/api/subjects/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.gradeLevel").exists());
    }

    @Test
    void deleteSubject_returns204() throws Exception {
        mockMvc.perform(delete("/api/subjects/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSubject_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Subject not found with id: 99"))
                .when(subjectService).delete(99L);

        mockMvc.perform(delete("/api/subjects/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Subject not found with id: 99"));
    }

    private SubjectRequest sampleRequest() {
        return new SubjectRequest("Mathematics", "Grade 10");
    }

    private SubjectResponse sampleResponse(Long id) {
        return new SubjectResponse(
                id,
                "Mathematics",
                "Grade 10",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
