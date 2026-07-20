package com.vikas.studentperformancedossier.dto;

public record SubjectAverageResponse(
        Long subjectId,
        String subjectName,
        Double averagePercentage
) {
}
