package com.zakat.service.dto;

import com.zakat.enums.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean active
) {
}
