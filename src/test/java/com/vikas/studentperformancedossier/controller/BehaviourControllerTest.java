package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.BehaviourRequest;
import com.vikas.studentperformancedossier.dto.BehaviourResponse;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.service.BehaviourService;
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

@WebMvcTest(BehaviourController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class BehaviourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BehaviourService behaviourService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllBehaviours_returnsList() throws Exception {
        when(behaviourService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/behaviours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].overallBehaviour").value(4));
    }

    @Test
    void getAllBehaviours_whenEmpty_returnsEmptyList() throws Exception {
        when(behaviourService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/behaviours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBehaviourById_whenFound_returnsBehaviour() throws Exception {
        when(behaviourService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/behaviours/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.attention").value(4));
    }

    @Test
    void getBehaviourById_whenMissing_returns404() throws Exception {
        when(behaviourService.findById(99L))
                .thenThrow(new EntityNotFoundException("Behaviour record not found with id: 99"));

        mockMvc.perform(get("/api/behaviours/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Behaviour record not found with id: 99"));
    }

    @Test
    void createBehaviour_whenValid_returns201() throws Exception {
        BehaviourRequest request = sampleRequest();
        when(behaviourService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createBehaviour_whenAttentionMissing_returns400() throws Exception {
        BehaviourRequest invalidRequest = new BehaviourRequest(
                null, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, null, null, null, null, 1L);

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.attention").exists());
    }

    @Test
    void createBehaviour_whenOverallBehaviourAboveRange_returns400() throws Exception {
        BehaviourRequest invalidRequest = new BehaviourRequest(
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, null, null, null, null, 1L);

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.overallBehaviour").exists());
    }

    @Test
    void createBehaviour_whenStudentIdMissing_returns400() throws Exception {
        BehaviourRequest invalidRequest = new BehaviourRequest(
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, null, null, null, null, null);

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void createBehaviour_whenStudentNotFound_returns404() throws Exception {
        BehaviourRequest request = sampleRequest();
        when(behaviourService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("Student not found with id: 1"));

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 1"));
    }

    @Test
    void createBehaviour_whenDuplicate_returns409() throws Exception {
        BehaviourRequest request = sampleRequest();
        when(behaviourService.create(eq(request)))
                .thenThrow(new DuplicateResourceException("A behaviour record already exists for student id 1"));

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A behaviour record already exists for student id 1"));
    }

    @Test
    void updateBehaviour_whenValid_returns200() throws Exception {
        BehaviourRequest request = sampleRequest();
        when(behaviourService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/behaviours/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateBehaviour_whenMissing_returns404() throws Exception {
        BehaviourRequest request = sampleRequest();
        when(behaviourService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("Behaviour record not found with id: 99"));

        mockMvc.perform(put("/api/behaviours/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Behaviour record not found with id: 99"));
    }

    @Test
    void updateBehaviour_whenInvalid_returns400WithFieldErrors() throws Exception {
        BehaviourRequest invalidRequest = new BehaviourRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/behaviours/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.attention").exists())
                .andExpect(jsonPath("$.errors.participation").exists())
                .andExpect(jsonPath("$.errors.discipline").exists())
                .andExpect(jsonPath("$.errors.homeworkResponsibility").exists())
                .andExpect(jsonPath("$.errors.communicationSkills").exists())
                .andExpect(jsonPath("$.errors.confidence").exists())
                .andExpect(jsonPath("$.errors.teamwork").exists())
                .andExpect(jsonPath("$.errors.curiosity").exists())
                .andExpect(jsonPath("$.errors.leadership").exists())
                .andExpect(jsonPath("$.errors.criticalThinking").exists())
                .andExpect(jsonPath("$.errors.overallBehaviour").exists())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void deleteBehaviour_returns204() throws Exception {
        mockMvc.perform(delete("/api/behaviours/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBehaviour_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Behaviour record not found with id: 99"))
                .when(behaviourService).delete(99L);

        mockMvc.perform(delete("/api/behaviours/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Behaviour record not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createBehaviour_whenTeacherRole_returns201() throws Exception {
        BehaviourRequest request = sampleRequest();
        when(behaviourService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createBehaviour_whenStudentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/behaviours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void deleteBehaviour_whenTeacherRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/behaviours/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getAllBehaviours_whenStudentRole_returnsOnlyOwnBehaviour() throws Exception {
        when(behaviourService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/behaviours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getBehaviourById_whenStudentAccessesAnotherStudentsBehaviour_returns404() throws Exception {
        when(behaviourService.findById(1L))
                .thenThrow(new EntityNotFoundException("Behaviour record not found with id: 1"));

        mockMvc.perform(get("/api/behaviours/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Behaviour record not found with id: 1"));
    }

    private BehaviourRequest sampleRequest() {
        return new BehaviourRequest(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                "Participates actively.", "Good listener.", "Time management.", "None", 1L);
    }

    private BehaviourResponse sampleResponse(Long id) {
        return new BehaviourResponse(
                id,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                "Participates actively.", "Good listener.", "Time management.", "None",
                1L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
