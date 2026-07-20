package com.vikas.studentperformancedossier.dto;

import java.util.List;

public record DossierResponse(
        Long studentId,
        String studentName,
        Double overallAveragePercentage,
        List<SubjectAverageResponse> subjectAverages,
        List<ExamSummaryResponse> examSummaries
) {
}
