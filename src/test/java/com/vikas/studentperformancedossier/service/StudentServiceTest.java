package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
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

    @Mock
    private SchoolClassRepository schoolClassRepository;

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
                "S-100",
                1L,
                null, null, null, null, null, null, null, null
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
    void create_whenSchoolClassNotFound_throwsEntityNotFoundException() {
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.empty());
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicates_savesStudent() {
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.empty());
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existingSchoolClass(1L)));
        when(studentRepository.save(any(Student.class)))
                .thenReturn(existingStudent(1L, "ada@example.com", "S-100"));

        StudentResponse response = studentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.schoolClassId()).isEqualTo(1L);
    }

    @Test
    void create_whenEmailNull_skipsEmailUniquenessCheckAndSaves() {
        StudentRequest requestWithoutEmail = new StudentRequest(
                "Ada", "Lovelace", null, null,
                LocalDate.of(2020, 1, 1), "S-100", 1L,
                "2024-25", "Father", "9990000000", "Mother", "9990000001",
                "123 Main St", "Father", "9990000000"
        );
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.empty());
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existingSchoolClass(1L)));
        when(studentRepository.save(any(Student.class)))
                .thenReturn(existingStudent(1L, null, "S-100"));

        StudentResponse response = studentService.create(requestWithoutEmail);

        assertThat(response.id()).isEqualTo(1L);
        verify(studentRepository, never()).findByEmail(any());
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
    void update_whenSchoolClassNotFound_throwsEntityNotFoundException() {
        Student self = existingStudent(1L, "ada@example.com", "S-100");
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(self));
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.of(self));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(self));
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_whenEmailAndStudentNumberBelongToSameStudent_updatesSuccessfully() {
        Student self = existingStudent(1L, "ada@example.com", "S-100");
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(self));
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.of(self));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(self));
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(existingSchoolClass(1L)));
        when(studentRepository.save(any(Student.class))).thenReturn(self);

        StudentResponse response = studentService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(studentRepository).save(self);
    }

    @Test
    void update_whenStudentNotFound_throwsEntityNotFoundException() {
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber("S-100")).thenReturn(Optional.empty());
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void delete_whenStudentNotFound_throwsEntityNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(studentRepository, never()).delete(any());
    }

    @Test
    void delete_whenStudentExists_deletesStudent() {
        Student existing = existingStudent(1L, "ada@example.com", "S-100");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        studentService.delete(1L);

        verify(studentRepository).delete(existing);
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
        student.setSchoolClass(existingSchoolClass(1L));
        return student;
    }

    private SchoolClass existingSchoolClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade("Grade 10");
        schoolClass.setSection("A");
        return schoolClass;
    }
}
