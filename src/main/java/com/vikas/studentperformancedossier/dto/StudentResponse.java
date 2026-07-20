package com.vikas.studentperformancedossier.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth,
        LocalDate enrollmentDate,
        String studentNumber,
        Long schoolClassId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
