package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.MarkRequest;
import com.vikas.studentperformancedossier.dto.MarkResponse;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.Mark;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.ExamRepository;
import com.vikas.studentperformancedossier.repository.MarkRepository;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import com.vikas.studentperformancedossier.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkService {

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;
    private final CurrentUserProvider currentUserProvider;

    public MarkService(MarkRepository markRepository, StudentRepository studentRepository,
                        ExamRepository examRepository, CurrentUserProvider currentUserProvider) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.examRepository = examRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<MarkResponse> findAll() {
        User currentUser = currentUserProvider.getCurrentUser();
        List<Mark> marks = currentUser.getRole() == Role.STUDENT
                ? findMarksForLinkedStudent(currentUser)
                : markRepository.findAll();
        return marks.stream()
                .map(this::toResponse)
                .toList();
    }

    public MarkResponse findById(Long id) {
        Mark mark = findEntityById(id);
        User currentUser = currentUserProvider.getCurrentUser();

        if (currentUser.getRole() == Role.STUDENT && !belongsToLinkedStudent(mark, currentUser)) {
            // Same "not found" as a genuinely missing id - a distinguishable 403 here would let a
            // student enumerate which mark ids exist and belong to other students.
            throw new EntityNotFoundException("Mark not found with id: " + id);
        }

        return toResponse(mark);
    }

    private List<Mark> findMarksForLinkedStudent(User currentUser) {
        Student student = currentUser.getStudent();
        return student == null ? List.of() : markRepository.findByStudent_Id(student.getId());
    }

    private boolean belongsToLinkedStudent(Mark mark, User currentUser) {
        Student student = currentUser.getStudent();
        return student != null && mark.getStudent().getId().equals(student.getId());
    }

    public MarkResponse create(MarkRequest request) {
        ensureUnique(request, null);
        Student student = findStudentById(request.studentId());
        Exam exam = findExamById(request.examId());
        Mark mark = new Mark();
        applyRequest(mark, request, student, exam);
        return toResponse(markRepository.save(mark));
    }

    public MarkResponse update(Long id, MarkRequest request) {
        ensureUnique(request, id);
        Mark existing = findEntityById(id);
        Student student = findStudentById(request.studentId());
        Exam exam = findExamById(request.examId());
        applyRequest(existing, request, student, exam);
        return toResponse(markRepository.save(existing));
    }

    public void delete(Long id) {
        markRepository.delete(findEntityById(id));
    }

    private void ensureUnique(MarkRequest request, Long excludingId) {
        markRepository.findByStudent_IdAndExam_Id(request.studentId(), request.examId())
                .filter(existing -> isDifferentRecord(existing, excludingId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A mark already exists for student id " + request.studentId()
                                    + " and exam id " + request.examId());
                });
    }

    private boolean isDifferentRecord(Mark existing, Long excludingId) {
        return excludingId == null || !existing.getId().equals(excludingId);
    }

    private Mark findEntityById(Long id) {
        return markRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mark not found with id: " + id));
    }

    private Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));
    }

    private Exam findExamById(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found with id: " + examId));
    }

    private void applyRequest(Mark mark, MarkRequest request, Student student, Exam exam) {
        mark.setObtainedMarks(request.obtainedMarks());
        mark.setMaximumMarks(request.maximumMarks());
        mark.setGrade(request.grade());
        mark.setRemarks(request.remarks());
        mark.setStudent(student);
        mark.setExam(exam);
    }

    private MarkResponse toResponse(Mark mark) {
        return new MarkResponse(
                mark.getId(),
                mark.getObtainedMarks(),
                mark.getMaximumMarks(),
                mark.getGrade(),
                mark.getRemarks(),
                mark.getStudent().getId(),
                mark.getExam().getId(),
                mark.getCreatedAt(),
                mark.getUpdatedAt()
        );
    }
}
