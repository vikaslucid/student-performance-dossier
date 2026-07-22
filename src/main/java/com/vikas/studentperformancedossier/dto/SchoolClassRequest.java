package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SchoolClassRequest(
        @NotBlank String grade,
        String stream,
        @NotBlank String section,
        @NotNull Long schoolId
) {
}
