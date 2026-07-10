package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<StudentResponse> findAll() {
        return studentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public StudentResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public StudentResponse create(StudentRequest request) {
        Student student = new Student();
        applyRequest(student, request);
        return toResponse(studentRepository.save(student));
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student existing = findEntityById(id);
        applyRequest(existing, request);
        return toResponse(studentRepository.save(existing));
    }

    public void delete(Long id) {
        studentRepository.delete(findEntityById(id));
    }

    private Student findEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));
    }

    private void applyRequest(Student student, StudentRequest request) {
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setDateOfBirth(request.dateOfBirth());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setStudentNumber(request.studentNumber());
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getDateOfBirth(),
                student.getEnrollmentDate(),
                student.getStudentNumber(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
