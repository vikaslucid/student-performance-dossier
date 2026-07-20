package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull @PastOrPresent LocalDate enrollmentDate,
        @NotBlank String studentNumber,
        @NotNull Long schoolClassId
) {
}
