package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyCodeSendRequest(
        @NotBlank(message = "account is required") String account,
        @NotBlank(message = "contact is required") String contact,
        @NotBlank(message = "scene is required") String scene
) {
}
