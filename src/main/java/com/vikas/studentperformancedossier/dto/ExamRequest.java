package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExamRequest(
        @NotBlank String name,
        @NotNull LocalDate examDate,
        @NotNull Long schoolClassId,
        @NotNull Long subjectId
) {
}
