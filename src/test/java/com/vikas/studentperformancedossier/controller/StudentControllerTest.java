package com.vikas.studentperformancedossier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vikas.studentperformancedossier.config.SecurityConfig;
import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(StudentController.class)
@Import(SecurityConfig.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    @Test
    void getAllStudents_returnsList() throws Exception {
        when(studentService.findAll()).thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("ada@example.com"));
    }

    @Test
    void getAllStudents_whenEmpty_returnsEmptyList() throws Exception {
        when(studentService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getStudentById_whenFound_returnsStudent() throws Exception {
        when(studentService.findById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/students/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.studentNumber").value("S-100"));
    }

    @Test
    void getStudentById_whenMissing_returns404() throws Exception {
        when(studentService.findById(99L))
                .thenThrow(new EntityNotFoundException("Student not found with id: 99"));

        mockMvc.perform(get("/api/students/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 99"));
    }

    @Test
    void createStudent_whenValid_returns201() throws Exception {
        StudentRequest request = sampleRequest();
        when(studentService.create(eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void createStudent_whenInvalid_returns400WithFieldErrors() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "",
                "Lovelace",
                "not-an-email",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
                "S-100",
                1L
        );

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    void createStudent_whenLastNameBlank_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100",
                1L
        );

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").exists());
    }

    @Test
    void createStudent_whenStudentNumberBlank_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "",
                1L
        );

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentNumber").exists());
    }

    @Test
    void createStudent_whenSchoolClassIdMissing_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100",
                null
        );

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolClassId").exists());
    }

    @Test
    void createStudent_whenEnrollmentDateInFuture_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.now().plusDays(1),
                "S-100",
                1L
        );

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.enrollmentDate").exists());
    }

    @Test
    void createStudent_whenDateOfBirthNotInPast_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.now(),
                LocalDate.of(2020, 1, 1),
                "S-100",
                1L
        );

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    void createStudent_whenDuplicate_returns409() throws Exception {
        StudentRequest request = sampleRequest();
        when(studentService.create(eq(request)))
                .thenThrow(new DuplicateResourceException("A student with email 'ada@example.com' already exists"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A student with email 'ada@example.com' already exists"));
    }

    @Test
    void createStudent_whenSchoolClassNotFound_returns404() throws Exception {
        StudentRequest request = sampleRequest();
        when(studentService.create(eq(request)))
                .thenThrow(new EntityNotFoundException("School class not found with id: 1"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("School class not found with id: 1"));
    }

    @Test
    void updateStudent_whenValid_returns200() throws Exception {
        StudentRequest request = sampleRequest();
        when(studentService.update(eq(1L), eq(request))).thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateStudent_whenMissing_returns404() throws Exception {
        StudentRequest request = sampleRequest();
        when(studentService.update(eq(99L), eq(request)))
                .thenThrow(new EntityNotFoundException("Student not found with id: 99"));

        mockMvc.perform(put("/api/students/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 99"));
    }

    @Test
    void updateStudent_whenDuplicate_returns409() throws Exception {
        StudentRequest request = sampleRequest();
        when(studentService.update(eq(1L), eq(request)))
                .thenThrow(new DuplicateResourceException("A student with email 'ada@example.com' already exists"));

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A student with email 'ada@example.com' already exists"));
    }

    @Test
    void updateStudent_whenInvalid_returns400WithFieldErrors() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "",
                "Lovelace",
                "not-an-email",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
                "S-100",
                1L
        );

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    void updateStudent_whenLastNameBlank_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100",
                1L
        );

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").exists());
    }

    @Test
    void updateStudent_whenStudentNumberBlank_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "",
                1L
        );

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentNumber").exists());
    }

    @Test
    void updateStudent_whenSchoolClassIdMissing_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100",
                null
        );

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolClassId").exists());
    }

    @Test
    void updateStudent_whenEnrollmentDateInFuture_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.now().plusDays(1),
                "S-100",
                1L
        );

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.enrollmentDate").exists());
    }

    @Test
    void updateStudent_whenDateOfBirthNotInPast_returns400() throws Exception {
        StudentRequest invalidRequest = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.now(),
                LocalDate.of(2020, 1, 1),
                "S-100",
                1L
        );

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    void deleteStudent_returns204() throws Exception {
        mockMvc.perform(delete("/api/students/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteStudent_whenMissing_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Student not found with id: 99"))
                .when(studentService).delete(99L);

        mockMvc.perform(delete("/api/students/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Student not found with id: 99"));
    }

    private StudentRequest sampleRequest() {
        return new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100",
                1L
        );
    }

    private StudentResponse sampleResponse(Long id) {
        return new StudentResponse(
                id,
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100",
                1L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
