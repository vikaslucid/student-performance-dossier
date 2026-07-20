package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank String name,
        @NotBlank String gradeLevel
) {
}
