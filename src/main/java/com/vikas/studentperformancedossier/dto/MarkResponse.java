package com.vikas.studentperformancedossier.dto;

import java.time.LocalDateTime;

public record MarkResponse(
        Long id,
        Integer concept,
        Integer application,
        Integer accuracy,
        Integer homework,
        Integer test,
        int total,
        double percentage,
        String grade,
        String remarks,
        Long studentId,
        Long examId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
