package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.MarkRequest;
import com.vikas.studentperformancedossier.dto.MarkResponse;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.service.MarkService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarkController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class MarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarkService markService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllMarks_returnsList() throws Exception {
        when(markService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/marks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].total").value(20));
    }

    @Test
    void getAllMarks_whenEmpty_returnsEmptyList() throws Exception {
        when(markService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/marks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getMarkById_whenFound_returnsMark() throws Exception {
        when(markService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/marks/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.grade").value("A2"));
    }

    @Test
    void getMarkById_whenMissing_returns404() throws Exception {
        when(markService.findById(99L))
                .thenThrow(new EntityNotFoundException("Mark not found with id: 99"));

        mockMvc.perform(get("/api/marks/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Mark not found with id: 99"));
    }

    @Test
    void createMark_whenValid_returns201() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.total").value(20));
    }

    @Test
    void createMark_whenConceptMissing_returns400() throws Exception {
        MarkRequest invalidRequest = new MarkRequest(null, 4, 4, 4, 4, "Well done", 1L, 2L);

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.concept").exists());
    }

    @Test
    void createMark_whenConceptBelowRange_returns400() throws Exception {
        MarkRequest invalidRequest = new MarkRequest(-1, 4, 4, 4, 4, "Well done", 1L, 2L);

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.concept").exists());
    }

    @Test
    void createMark_whenTestAboveRange_returns400() throws Exception {
        MarkRequest invalidRequest = new MarkRequest(4, 4, 4, 4, 6, "Well done", 1L, 2L);

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.test").exists());
    }

    @Test
    void createMark_whenStudentIdMissing_returns400() throws Exception {
        MarkRequest invalidRequest = new MarkRequest(4, 4, 4, 4, 4, "Well done", null, 2L);

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void createMark_whenExamIdMissing_returns400() throws Exception {
        MarkRequest invalidRequest = new MarkRequest(4, 4, 4, 4, 4, "Well done", 1L, null);

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.examId").exists());
    }

    @Test
    void createMark_whenStudentNotFound_returns404() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("Student not found with id: 1"));

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 1"));
    }

    @Test
    void createMark_whenExamNotFound_returns404() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("Exam not found with id: 2"));

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exam not found with id: 2"));
    }

    @Test
    void createMark_whenDuplicate_returns409() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.create(eq(request)))
                .thenThrow(new DuplicateResourceException("A mark already exists for student id 1 and exam id 2"));

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A mark already exists for student id 1 and exam id 2"));
    }

    @Test
    void updateMark_whenValid_returns200() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/marks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateMark_whenMissing_returns404() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("Mark not found with id: 99"));

        mockMvc.perform(put("/api/marks/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Mark not found with id: 99"));
    }

    @Test
    void updateMark_whenDuplicate_returns409() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.update(eq(1L), eq(request)))
                .thenThrow(new DuplicateResourceException("A mark already exists for student id 1 and exam id 2"));

        mockMvc.perform(put("/api/marks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMark_whenInvalid_returns400WithFieldErrors() throws Exception {
        MarkRequest invalidRequest = new MarkRequest(null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/marks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.concept").exists())
                .andExpect(jsonPath("$.errors.application").exists())
                .andExpect(jsonPath("$.errors.accuracy").exists())
                .andExpect(jsonPath("$.errors.homework").exists())
                .andExpect(jsonPath("$.errors.test").exists())
                .andExpect(jsonPath("$.errors.studentId").exists())
                .andExpect(jsonPath("$.errors.examId").exists());
    }

    @Test
    void deleteMark_returns204() throws Exception {
        mockMvc.perform(delete("/api/marks/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMark_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Mark not found with id: 99"))
                .when(markService).delete(99L);

        mockMvc.perform(delete("/api/marks/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Mark not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createMark_whenTeacherRole_returns201() throws Exception {
        MarkRequest request = sampleRequest();
        when(markService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createMark_whenStudentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/marks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void deleteMark_whenTeacherRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/marks/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getAllMarks_whenStudentRole_returnsOnlyOwnMarks() throws Exception {
        when(markService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/marks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMarkById_whenStudentAccessesAnotherStudentsMark_returns404() throws Exception {
        when(markService.findById(1L))
                .thenThrow(new EntityNotFoundException("Mark not found with id: 1"));

        mockMvc.perform(get("/api/marks/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Mark not found with id: 1"));
    }

    private MarkRequest sampleRequest() {
        return new MarkRequest(4, 4, 4, 4, 4, "Well done", 1L, 2L);
    }

    private MarkResponse sampleResponse(Long id) {
        return new MarkResponse(
                id,
                4,
                4,
                4,
                4,
                4,
                20,
                80.0,
                "A2",
                "Well done",
                1L,
                2L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
