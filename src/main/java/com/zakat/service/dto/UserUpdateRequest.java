package com.zakat.service.dto;

import com.zakat.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        String password,
        UserRole role,
        Boolean active
) {
}

