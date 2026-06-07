package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordResetRequest(
        @NotBlank(message = "account is required") String account,
        @NotBlank(message = "contact is required") String contact,
        @NotBlank(message = "verifyCode is required") String verifyCode,
        @NotBlank(message = "newPassword is required") String newPassword,
        @NotBlank(message = "confirmPassword is required") String confirmPassword
) {
}
