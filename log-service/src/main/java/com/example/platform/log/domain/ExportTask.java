package com.example.platform.log.domain;

import java.time.Instant;

public record ExportTask(
        String exportId,
        String format,
        String query,
        String status,
        String downloadPath,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt,
        Integer recordCount,
        String failureReason
) {
}
