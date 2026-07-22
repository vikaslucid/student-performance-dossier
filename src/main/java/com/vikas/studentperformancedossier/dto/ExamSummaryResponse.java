package com.vikas.studentperformancedossier.dto;

import java.time.LocalDate;

public record ExamSummaryResponse(
        Long examId,
        String examName,
        LocalDate examDate,
        Long subjectId,
        String subjectName,
        Integer concept,
        Integer application,
        Integer accuracy,
        Integer homework,
        Integer test,
        Integer obtainedMarks,
        Integer maximumMarks,
        Double percentage,
        boolean passed,
        String grade
) {
}
