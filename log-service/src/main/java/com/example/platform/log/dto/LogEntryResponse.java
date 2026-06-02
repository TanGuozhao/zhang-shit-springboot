package com.example.platform.log.dto;

import java.time.Instant;

public record LogEntryResponse(
        String logId,
        String serviceName,
        String traceId,
        String level,
        String message,
        Instant timestamp
) {
}
