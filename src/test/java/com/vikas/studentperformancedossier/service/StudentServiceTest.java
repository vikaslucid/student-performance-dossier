package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private StudentRequest request;

    @BeforeEach
    void setUp() {
        request = new StudentRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2020, 1, 1),
                "S-100"
        );
    }

    @Test
    void create_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        when(studentRepository.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(existingStudent(2L, "ada@example.com", "S-999")));

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ada@example.com");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_whenStudentNumberAlreadyExists_throwsDuplicateResourceException() {
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber("S-100"))
                .thenReturn(Optional.of(existingStudent(2L, "other@example.com", "S-100")));

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("S-100");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicates_savesStudent() {
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class)))
                .thenReturn(existingStudent(1L, "ada@example.com", "S-100"));

        StudentResponse response = studentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void update_whenEmailBelongsToAnotherStudent_throwsDuplicateResourceException() {
        when(studentRepository.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(existingStudent(2L, "ada@example.com", "S-999")));

        assertThatThrownBy(() -> studentService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ada@example.com");

        verify(studentRepository, never()).findById(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_whenStudentNumberBelongsToAnotherStudent_throwsDuplicateResourceException() {
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber("S-100"))
                .thenReturn(Optional.of(existingStudent(2L, "other@example.com", "S-100")));

        assertThatThrownBy(() -> studentService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("S-100");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_whenEmailAndStudentNumberBelongToSameStudent_updatesSuccessfully() {
        Student self = existingStudent(1L, "ada@example.com", "S-100");
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(self));
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.of(self));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(self));
        when(studentRepository.save(any(Student.class))).thenReturn(self);

        StudentResponse response = studentService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(studentRepository).save(self);
    }

    private Student existingStudent(Long id, String email, String studentNumber) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("Existing");
        student.setLastName("Student");
        student.setEmail(email);
        student.setDateOfBirth(LocalDate.of(1990, 1, 1));
        student.setEnrollmentDate(LocalDate.of(2020, 1, 1));
        student.setStudentNumber(studentNumber);
        return student;
    }
}
