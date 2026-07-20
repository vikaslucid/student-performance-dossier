package com.vikas.studentperformancedossier.dto;

import java.time.LocalDateTime;

public record MarkResponse(
        Long id,
        Integer obtainedMarks,
        Integer maximumMarks,
        String grade,
        String remarks,
        Long studentId,
        Long examId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
