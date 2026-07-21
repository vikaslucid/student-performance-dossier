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
        String session,
        String fatherName,
        String fatherMobile,
        String motherName,
        String motherMobile,
        String address,
        String primaryParent,
        String primaryParentMobile,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
