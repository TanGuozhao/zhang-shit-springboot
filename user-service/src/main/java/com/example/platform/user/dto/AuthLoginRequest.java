package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @NotBlank(message = "account is required") String account,
        @NotBlank(message = "password is required") String password
) {
}
