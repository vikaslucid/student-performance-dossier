package com.vikas.studentperformancedossier.dto;

import java.time.LocalDateTime;

public record SchoolClassResponse(
        Long id,
        String grade,
        String stream,
        String section,
        Long schoolId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
