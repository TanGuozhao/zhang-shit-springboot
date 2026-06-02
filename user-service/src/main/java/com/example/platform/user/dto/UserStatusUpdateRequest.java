package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserStatusUpdateRequest(
        @NotBlank(message = "status is required") String status
) {
}
