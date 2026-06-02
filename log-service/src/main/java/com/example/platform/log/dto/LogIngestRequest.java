package com.example.platform.log.dto;

import jakarta.validation.constraints.NotBlank;

public record LogIngestRequest(
        @NotBlank(message = "serviceName is required") String serviceName,
        @NotBlank(message = "traceId is required") String traceId,
        @NotBlank(message = "level is required") String level,
        @NotBlank(message = "message is required") String message
) {
}
