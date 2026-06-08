package com.example.platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeLoginRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "verifyCode is required")
        String verifyCode,
        Boolean autoRegister,
        String userName
) {
}
