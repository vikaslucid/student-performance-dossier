package com.vikas.studentperformancedossier.dto;

import com.vikas.studentperformancedossier.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role,
        Long studentId
) {
}
