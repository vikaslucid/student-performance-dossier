package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.BehaviourResponse;
import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import com.vikas.studentperformancedossier.entity.Behaviour;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.Mark;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.repository.BehaviourRepository;
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
    private static final int MAXIMUM_TOTAL = 25;

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final BehaviourRepository behaviourRepository;
    private final CurrentUserProvider currentUserProvider;

    public DossierService(MarkRepository markRepository, StudentRepository studentRepository,
                           BehaviourRepository behaviourRepository, CurrentUserProvider currentUserProvider) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.behaviourRepository = behaviourRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public DossierResponse getDossier(Long studentId) {
        Student student = findAccessibleStudent(studentId);
        List<Mark> marks = markRepository.findByStudent_Id(studentId);
        SchoolClass schoolClass = student.getSchoolClass();

        List<ExamSummaryResponse> examSummaries = marks.stream()
                .map(this::toExamSummary)
                .toList();

        Double overallAverage = average(marks);

        return new DossierResponse(
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getStudentNumber(),
                schoolClass.getSchool().getName(),
                schoolClass.getGrade(),
                schoolClass.getStream(),
                schoolClass.getSection(),
                student.getSession(),
                overallAverage,
                overallGrade(overallAverage),
                subjectAverages(marks),
                examSummaries,
                behaviourRepository.findByStudent_Id(studentId).map(this::toBehaviourResponse).orElse(null)
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
        double percentage = mark.getPercentage();

        return new ExamSummaryResponse(
                exam.getId(),
                exam.getName(),
                exam.getExamDate(),
                subject.getId(),
                subject.getName(),
                mark.getConcept(),
                mark.getApplication(),
                mark.getAccuracy(),
                mark.getHomework(),
                mark.getTest(),
                mark.getTotal(),
                MAXIMUM_TOTAL,
                round(percentage),
                percentage >= PASS_THRESHOLD_PERCENTAGE,
                mark.getGrade()
        );
    }

    private BehaviourResponse toBehaviourResponse(Behaviour behaviour) {
        return new BehaviourResponse(
                behaviour.getId(),
                behaviour.getAttention(),
                behaviour.getParticipation(),
                behaviour.getDiscipline(),
                behaviour.getHomeworkResponsibility(),
                behaviour.getCommunicationSkills(),
                behaviour.getConfidence(),
                behaviour.getTeamwork(),
                behaviour.getCuriosity(),
                behaviour.getLeadership(),
                behaviour.getCriticalThinking(),
                behaviour.getOverallBehaviour(),
                behaviour.getAnecdotalObservation(),
                behaviour.getStrength(),
                behaviour.getNeedsImprovement(),
                behaviour.getParentSupportRequired(),
                behaviour.getStudent().getId(),
                behaviour.getCreatedAt(),
                behaviour.getUpdatedAt()
        );
    }

    private Double average(List<Mark> marks) {
        if (marks.isEmpty()) {
            return null;
        }
        double sum = marks.stream().mapToDouble(Mark::getPercentage).sum();
        return round(sum / marks.size());
    }

    // A separate, coarser band from Mark.getGrade() - this is the PTM report's overall-standing
    // grade (A+/A/B/C/Needs Attention), distinct from the per-subject A1/A2/B1/B2/C/D grade.
    private String overallGrade(Double percentage) {
        if (percentage == null) {
            return null;
        }
        if (percentage >= 90) {
            return "A+";
        }
        if (percentage >= 75) {
            return "A";
        }
        if (percentage >= 60) {
            return "B";
        }
        if (percentage >= 45) {
            return "C";
        }
        return "Needs Attention";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
