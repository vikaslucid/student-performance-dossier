package com.vikas.studentperformancedossier.dto;

import com.vikas.studentperformancedossier.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
