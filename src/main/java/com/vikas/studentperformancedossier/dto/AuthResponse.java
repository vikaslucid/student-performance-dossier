package com.vikas.studentperformancedossier.dto;

import com.vikas.studentperformancedossier.entity.Role;

public record AuthResponse(
        String token,
        String username,
        Role role
) {
}
