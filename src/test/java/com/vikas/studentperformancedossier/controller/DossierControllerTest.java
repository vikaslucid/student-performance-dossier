package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import com.vikas.studentperformancedossier.pdf.DossierPdfGenerator;
import com.vikas.studentperformancedossier.repository.UserRepository;
import com.vikas.studentperformancedossier.service.DossierService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DossierController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class DossierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DossierService dossierService;

    @MockitoBean
    private DossierPdfGenerator dossierPdfGenerator;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getDossier_whenFound_returnsDossierJson() throws Exception {
        when(dossierService.getDossier(1L)).thenReturn(sampleDossier());

        mockMvc.perform(get("/api/dossier/{studentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.studentName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.overallAveragePercentage").value(85.0))
                .andExpect(jsonPath("$.subjectAverages[0].subjectName").value("Mathematics"))
                .andExpect(jsonPath("$.examSummaries[0].passed").value(true));
    }

    @Test
    void getDossier_whenStudentNotFound_returns404() throws Exception {
        when(dossierService.getDossier(99L))
                .thenThrow(new EntityNotFoundException("Student not found with id: 99"));

        mockMvc.perform(get("/api/dossier/{studentId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getDossier_whenStudentAccessesAnotherStudentsDossier_returns404() throws Exception {
        when(dossierService.getDossier(1L))
                .thenThrow(new EntityNotFoundException("Student not found with id: 1"));

        mockMvc.perform(get("/api/dossier/{studentId}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getDossier_whenStudentAccessesOwnDossier_returns200() throws Exception {
        when(dossierService.getDossier(1L)).thenReturn(sampleDossier());

        mockMvc.perform(get("/api/dossier/{studentId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getDossier_whenTeacher_returns200() throws Exception {
        when(dossierService.getDossier(1L)).thenReturn(sampleDossier());

        mockMvc.perform(get("/api/dossier/{studentId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getDossierPdf_returnsValidPdfResponse() throws Exception {
        byte[] fakePdfBytes = "%PDF-1.4 fake content".getBytes(StandardCharsets.US_ASCII);
        when(dossierService.getDossier(1L)).thenReturn(sampleDossier());
        when(dossierPdfGenerator.generate(sampleDossier())).thenReturn(fakePdfBytes);

        mockMvc.perform(get("/api/dossier/{studentId}/pdf", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dossier-1.pdf\""))
                .andExpect(content().bytes(fakePdfBytes));
    }

    @Test
    void getDossierPdf_whenStudentNotFound_returns404() throws Exception {
        when(dossierService.getDossier(99L))
                .thenThrow(new EntityNotFoundException("Student not found with id: 99"));

        mockMvc.perform(get("/api/dossier/{studentId}/pdf", 99L))
                .andExpect(status().isNotFound());
    }

    private DossierResponse sampleDossier() {
        SubjectAverageResponse subjectAverage = new SubjectAverageResponse(1L, "Mathematics", 85.0);
        ExamSummaryResponse examSummary = new ExamSummaryResponse(
                1L, "Midterm", LocalDate.of(2026, 3, 1), 1L, "Mathematics", 85, 100, 85.0, true, "A");

        return new DossierResponse(1L, "Ada Lovelace", 85.0, List.of(subjectAverage), List.of(examSummary));
    }
}
