package com.vikas.studentperformancedossier.dto;

import java.time.LocalDateTime;

public record SubjectResponse(
        Long id,
        String name,
        String gradeLevel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
