package com.vikas.studentperformancedossier.dto;

import java.util.List;

public record DossierResponse(
        Long studentId,
        String studentName,
        String studentNumber,
        String schoolName,
        String grade,
        String stream,
        String section,
        String session,
        Double overallAveragePercentage,
        String overallGrade,
        List<SubjectAverageResponse> subjectAverages,
        List<ExamSummaryResponse> examSummaries,
        BehaviourResponse behaviour
) {
}
