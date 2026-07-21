package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.NotBlank;

public record SchoolRequest(
        @NotBlank String name,
        @NotBlank String address
) {
}
