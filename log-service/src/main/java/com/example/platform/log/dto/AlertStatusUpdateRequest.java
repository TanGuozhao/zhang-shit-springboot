package com.example.platform.log.dto;

import jakarta.validation.constraints.NotBlank;

public record AlertStatusUpdateRequest(
        @NotBlank(message = "status is required") String status
) {
}
