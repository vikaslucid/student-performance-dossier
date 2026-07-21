package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.SchoolRequest;
import com.vikas.studentperformancedossier.dto.SchoolResponse;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.service.SchoolService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
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

@WebMvcTest(SchoolController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class SchoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SchoolService schoolService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllSchools_returnsList() throws Exception {
        when(schoolService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Central High"));
    }

    @Test
    void getAllSchools_whenEmpty_returnsEmptyList() throws Exception {
        when(schoolService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSchoolById_whenFound_returnsSchool() throws Exception {
        when(schoolService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/schools/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address").value("123 Main St"));
    }

    @Test
    void getSchoolById_whenMissing_returns404() throws Exception {
        when(schoolService.findById(99L))
                .thenThrow(new EntityNotFoundException("School not found with id: 99"));

        mockMvc.perform(get("/api/schools/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School not found with id: 99"));
    }

    @Test
    void createSchool_whenValid_returns201() throws Exception {
        SchoolRequest request = sampleRequest();
        when(schoolService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Central High"));
    }

    @Test
    void createSchool_whenNameBlank_returns400() throws Exception {
        SchoolRequest invalidRequest = new SchoolRequest("", "123 Main St");

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createSchool_whenAddressBlank_returns400() throws Exception {
        SchoolRequest invalidRequest = new SchoolRequest("Central High", "");

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.address").exists());
    }

    @Test
    void updateSchool_whenValid_returns200() throws Exception {
        SchoolRequest request = sampleRequest();
        when(schoolService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/schools/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateSchool_whenMissing_returns404() throws Exception {
        SchoolRequest request = sampleRequest();
        when(schoolService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("School not found with id: 99"));

        mockMvc.perform(put("/api/schools/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School not found with id: 99"));
    }

    @Test
    void updateSchool_whenInvalid_returns400WithFieldErrors() throws Exception {
        SchoolRequest invalidRequest = new SchoolRequest("", "");

        mockMvc.perform(put("/api/schools/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.address").exists());
    }

    @Test
    void deleteSchool_returns204() throws Exception {
        mockMvc.perform(delete("/api/schools/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSchool_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("School not found with id: 99"))
                .when(schoolService).delete(99L);

        mockMvc.perform(delete("/api/schools/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createSchool_whenTeacherRole_returns403() throws Exception {
        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getAllSchools_whenStudentRole_returns200() throws Exception {
        when(schoolService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void getAllSchools_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isUnauthorized());
    }

    private SchoolRequest sampleRequest() {
        return new SchoolRequest("Central High", "123 Main St");
    }

    private SchoolResponse sampleResponse(Long id) {
        return new SchoolResponse(
                id,
                "Central High",
                "123 Main St",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
