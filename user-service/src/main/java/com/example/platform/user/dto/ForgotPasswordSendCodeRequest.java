package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordSendCodeRequest(
        @NotBlank(message = "account is required") String account,
        @NotBlank(message = "contact is required") String contact
) {
}
