package com.vikas.studentperformancedossier.dto;

import java.time.LocalDate;

public record ExamSummaryResponse(
        Long examId,
        String examName,
        LocalDate examDate,
        Long subjectId,
        String subjectName,
        Integer obtainedMarks,
        Integer maximumMarks,
        Double percentage,
        boolean passed,
        String grade
) {
}
