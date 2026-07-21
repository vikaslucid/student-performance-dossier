package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    public StudentService(StudentRepository studentRepository, SchoolClassRepository schoolClassRepository) {
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
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
        ensureUnique(request, null);
        SchoolClass schoolClass = findSchoolClassById(request.schoolClassId());
        Student student = new Student();
        applyRequest(student, request, schoolClass);
        return toResponse(studentRepository.save(student));
    }

    public StudentResponse update(Long id, StudentRequest request) {
        ensureUnique(request, id);
        Student existing = findEntityById(id);
        SchoolClass schoolClass = findSchoolClassById(request.schoolClassId());
        applyRequest(existing, request, schoolClass);
        return toResponse(studentRepository.save(existing));
    }

    public void delete(Long id) {
        studentRepository.delete(findEntityById(id));
    }

    private void ensureUnique(StudentRequest request, Long excludingId) {
        if (request.email() != null) {
            studentRepository.findByEmail(request.email())
                    .filter(existing -> isDifferentRecord(existing, excludingId))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException(
                                "A student with email '" + request.email() + "' already exists");
                    });
        }

        studentRepository.findByStudentNumber(request.studentNumber())
                .filter(existing -> isDifferentRecord(existing, excludingId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A student with student number '" + request.studentNumber() + "' already exists");
                });
    }

    private boolean isDifferentRecord(Student existing, Long excludingId) {
        return excludingId == null || !existing.getId().equals(excludingId);
    }

    private Student findEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));
    }

    private SchoolClass findSchoolClassById(Long schoolClassId) {
        return schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new EntityNotFoundException("School class not found with id: " + schoolClassId));
    }

    private void applyRequest(Student student, StudentRequest request, SchoolClass schoolClass) {
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setDateOfBirth(request.dateOfBirth());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setStudentNumber(request.studentNumber());
        student.setSchoolClass(schoolClass);
        student.setSession(request.session());
        student.setFatherName(request.fatherName());
        student.setFatherMobile(request.fatherMobile());
        student.setMotherName(request.motherName());
        student.setMotherMobile(request.motherMobile());
        student.setAddress(request.address());
        student.setPrimaryParent(request.primaryParent());
        student.setPrimaryParentMobile(request.primaryParentMobile());
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
                student.getSchoolClass().getId(),
                student.getSession(),
                student.getFatherName(),
                student.getFatherMobile(),
                student.getMotherName(),
                student.getMotherMobile(),
                student.getAddress(),
                student.getPrimaryParent(),
                student.getPrimaryParentMobile(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
