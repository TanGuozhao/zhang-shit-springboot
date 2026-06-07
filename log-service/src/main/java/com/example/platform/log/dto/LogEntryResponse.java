package com.example.platform.log.dto;

import com.example.platform.log.domain.AccessLogRecord;

import java.time.Instant;

public record LogEntryResponse(
        String logId,
        String serviceName,
        String traceId,
        String level,
        String message,
        Instant timestamp
) {

    public static LogEntryResponse from(AccessLogRecord record) {
        return new LogEntryResponse(
                record.logId(),
                record.serviceName(),
                record.traceId(),
                record.level(),
                record.message(),
                record.timestamp()
        );
    }
}
