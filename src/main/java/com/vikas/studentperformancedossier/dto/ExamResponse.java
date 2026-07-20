package com.vikas.studentperformancedossier.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExamResponse(
        Long id,
        String name,
        LocalDate examDate,
        Long schoolClassId,
        Long subjectId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
