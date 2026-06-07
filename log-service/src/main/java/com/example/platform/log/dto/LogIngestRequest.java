package com.example.platform.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;

public record LogIngestRequest(
        @NotBlank(message = "serviceName is required") String serviceName,
        @NotBlank(message = "traceId is required") String traceId,
        @NotBlank(message = "level is required") String level,
        @NotBlank(message = "message is required") String message,
        String path,
        @PositiveOrZero(message = "statusCode must be greater than or equal to 0") Integer statusCode,
        @PositiveOrZero(message = "latencyMs must be greater than or equal to 0") Long latencyMs,
        String requestId,
        String clientIp,
        Instant timestamp,
        Map<String, String> tags
) {
}
