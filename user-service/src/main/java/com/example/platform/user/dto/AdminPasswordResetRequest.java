package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminPasswordResetRequest(
        @NotBlank(message = "newPassword is required") String newPassword
) {
}
