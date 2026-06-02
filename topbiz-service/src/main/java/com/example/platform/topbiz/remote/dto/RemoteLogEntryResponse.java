package com.example.platform.topbiz.remote.dto;

import java.time.Instant;

public record RemoteLogEntryResponse(
        String logId,
        String serviceName,
        String traceId,
        String level,
        String message,
        Instant timestamp
) {
}
