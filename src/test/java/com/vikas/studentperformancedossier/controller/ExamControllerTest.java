package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.dto.ExamRequest;
import com.vikas.studentperformancedossier.dto.ExamResponse;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.service.ExamService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(ExamController.class)
class ExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExamService examService;

    @Test
    void getAllExams_returnsList() throws Exception {
        when(examService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Midterm"));
    }

    @Test
    void getAllExams_whenEmpty_returnsEmptyList() throws Exception {
        when(examService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getExamById_whenFound_returnsExam() throws Exception {
        when(examService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/exams/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subjectId").value(2));
    }

    @Test
    void getExamById_whenMissing_returns404() throws Exception {
        when(examService.findById(99L))
                .thenThrow(new EntityNotFoundException("Exam not found with id: 99"));

        mockMvc.perform(get("/api/exams/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exam not found with id: 99"));
    }

    @Test
    void createExam_whenValid_returns201() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Midterm"));
    }

    @Test
    void createExam_whenNameBlank_returns400() throws Exception {
        ExamRequest invalidRequest = new ExamRequest("", LocalDate.of(2026, 3, 1), 1L, 2L);

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createExam_whenExamDateMissing_returns400() throws Exception {
        ExamRequest invalidRequest = new ExamRequest("Midterm", null, 1L, 2L);

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.examDate").exists());
    }

    @Test
    void createExam_whenSchoolClassIdMissing_returns400() throws Exception {
        ExamRequest invalidRequest = new ExamRequest("Midterm", LocalDate.of(2026, 3, 1), null, 2L);

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolClassId").exists());
    }

    @Test
    void createExam_whenSubjectIdMissing_returns400() throws Exception {
        ExamRequest invalidRequest = new ExamRequest("Midterm", LocalDate.of(2026, 3, 1), 1L, null);

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.subjectId").exists());
    }

    @Test
    void createExam_whenSchoolClassNotFound_returns404() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("School class not found with id: 1"));

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School class not found with id: 1"));
    }

    @Test
    void createExam_whenSubjectNotFound_returns404() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("Subject not found with id: 2"));

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Subject not found with id: 2"));
    }

    @Test
    void createExam_whenDuplicate_returns409() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.create(eq(request)))
                .thenThrow(new DuplicateResourceException(
                        "An exam named 'Midterm' already exists for this school class and subject"));

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "An exam named 'Midterm' already exists for this school class and subject"));
    }

    @Test
    void updateExam_whenValid_returns200() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/exams/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateExam_whenMissing_returns404() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("Exam not found with id: 99"));

        mockMvc.perform(put("/api/exams/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exam not found with id: 99"));
    }

    @Test
    void updateExam_whenDuplicate_returns409() throws Exception {
        ExamRequest request = sampleRequest();
        when(examService.update(eq(1L), eq(request)))
                .thenThrow(new DuplicateResourceException(
                        "An exam named 'Midterm' already exists for this school class and subject"));

        mockMvc.perform(put("/api/exams/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateExam_whenInvalid_returns400WithFieldErrors() throws Exception {
        ExamRequest invalidRequest = new ExamRequest("", null, null, null);

        mockMvc.perform(put("/api/exams/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.examDate").exists())
                .andExpect(jsonPath("$.errors.schoolClassId").exists())
                .andExpect(jsonPath("$.errors.subjectId").exists());
    }

    @Test
    void deleteExam_returns204() throws Exception {
        mockMvc.perform(delete("/api/exams/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExam_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Exam not found with id: 99"))
                .when(examService).delete(99L);

        mockMvc.perform(delete("/api/exams/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exam not found with id: 99"));
    }

    private ExamRequest sampleRequest() {
        return new ExamRequest("Midterm", LocalDate.of(2026, 3, 1), 1L, 2L);
    }

    private ExamResponse sampleResponse(Long id) {
        return new ExamResponse(
                id,
                "Midterm",
                LocalDate.of(2026, 3, 1),
                1L,
                2L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
