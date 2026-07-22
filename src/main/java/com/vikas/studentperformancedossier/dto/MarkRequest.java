package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MarkRequest(
        @NotNull @Min(0) @Max(5) Integer concept,
        @NotNull @Min(0) @Max(5) Integer application,
        @NotNull @Min(0) @Max(5) Integer accuracy,
        @NotNull @Min(0) @Max(5) Integer homework,
        @NotNull @Min(0) @Max(5) Integer test,
        String remarks,
        @NotNull Long studentId,
        @NotNull Long examId
) {
}
