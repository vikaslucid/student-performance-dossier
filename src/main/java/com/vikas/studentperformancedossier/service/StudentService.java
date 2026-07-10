package com.vikas.studentperformancedossier.service;

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

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));
    }

    public Student create(Student student) {
        return studentRepository.save(student);
    }

    public Student update(Long id, Student updated) {
        Student existing = findById(id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setEnrollmentDate(updated.getEnrollmentDate());
        existing.setStudentNumber(updated.getStudentNumber());
        return studentRepository.save(existing);
    }

    public void delete(Long id) {
        Student existing = findById(id);
        studentRepository.delete(existing);
    }
}
