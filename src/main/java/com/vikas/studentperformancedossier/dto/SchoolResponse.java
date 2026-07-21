package com.vikas.studentperformancedossier.dto;

import java.time.LocalDateTime;

public record SchoolResponse(
        Long id,
        String name,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
