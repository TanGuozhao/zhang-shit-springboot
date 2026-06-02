package com.example.platform.log.domain;

import java.time.Instant;

public record LogEntry(
        String logId,
        String serviceName,
        String traceId,
        String level,
        String message,
        Instant timestamp
) {
}
