package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.Mark;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.repository.MarkRepository;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import com.vikas.studentperformancedossier.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DossierService {

    private static final double PASS_THRESHOLD_PERCENTAGE = 40.0;

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserProvider currentUserProvider;

    public DossierService(MarkRepository markRepository, StudentRepository studentRepository,
                           CurrentUserProvider currentUserProvider) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public DossierResponse getDossier(Long studentId) {
        Student student = findAccessibleStudent(studentId);
        List<Mark> marks = markRepository.findByStudent_Id(studentId);

        List<ExamSummaryResponse> examSummaries = marks.stream()
                .map(this::toExamSummary)
                .toList();

        return new DossierResponse(
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                average(marks),
                subjectAverages(marks),
                examSummaries
        );
    }

    private Student findAccessibleStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));

        User currentUser = currentUserProvider.getCurrentUser();
        if (currentUser.getRole() == Role.STUDENT && !isOwnStudentRecord(currentUser, studentId)) {
            // Same "not found" as a genuinely missing id - see MarkService.findById for why a
            // distinguishable 403 here would be an information leak.
            throw new EntityNotFoundException("Student not found with id: " + studentId);
        }

        return student;
    }

    private boolean isOwnStudentRecord(User currentUser, Long studentId) {
        Student linked = currentUser.getStudent();
        return linked != null && linked.getId().equals(studentId);
    }

    private List<SubjectAverageResponse> subjectAverages(List<Mark> marks) {
        Map<Long, List<Mark>> bySubjectId = marks.stream()
                .collect(Collectors.groupingBy(mark -> mark.getExam().getSubject().getId()));

        return bySubjectId.values().stream()
                .map(subjectMarks -> {
                    Subject subject = subjectMarks.get(0).getExam().getSubject();
                    return new SubjectAverageResponse(subject.getId(), subject.getName(), average(subjectMarks));
                })
                .sorted(Comparator.comparing(SubjectAverageResponse::subjectName))
                .toList();
    }

    private ExamSummaryResponse toExamSummary(Mark mark) {
        Exam exam = mark.getExam();
        Subject subject = exam.getSubject();
        double percentage = percentageOf(mark);

        return new ExamSummaryResponse(
                exam.getId(),
                exam.getName(),
                exam.getExamDate(),
                subject.getId(),
                subject.getName(),
                mark.getObtainedMarks(),
                mark.getMaximumMarks(),
                round(percentage),
                percentage >= PASS_THRESHOLD_PERCENTAGE,
                mark.getGrade()
        );
    }

    private Double average(List<Mark> marks) {
        if (marks.isEmpty()) {
            return null;
        }
        double sum = marks.stream().mapToDouble(this::percentageOf).sum();
        return round(sum / marks.size());
    }

    private double percentageOf(Mark mark) {
        return mark.getObtainedMarks() * 100.0 / mark.getMaximumMarks();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
