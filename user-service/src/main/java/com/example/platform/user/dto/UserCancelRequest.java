package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCancelRequest(
        @NotBlank(message = "password is required") String password
) {
}
