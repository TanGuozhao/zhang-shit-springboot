package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank(message = "oldPassword is required") String oldPassword,
        @NotBlank(message = "newPassword is required") String newPassword,
        @NotBlank(message = "confirmPassword is required") String confirmPassword
) {
}
