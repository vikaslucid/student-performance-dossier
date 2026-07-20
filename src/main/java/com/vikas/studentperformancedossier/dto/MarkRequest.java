package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record MarkRequest(
        @NotNull @PositiveOrZero Integer obtainedMarks,
        @NotNull @Positive Integer maximumMarks,
        String grade,
        String remarks,
        @NotNull Long studentId,
        @NotNull Long examId
) {
}
