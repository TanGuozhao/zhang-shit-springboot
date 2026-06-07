package com.example.platform.log.dto;

import com.example.platform.log.domain.ExportTask;

import java.time.Instant;

public record ExportTaskResponse(
        String exportId,
        String format,
        String status,
        String downloadPath,
        Instant createdAt,
        Instant completedAt,
        Instant expiresAt,
        Integer recordCount,
        String failureReason
) {

    public static ExportTaskResponse from(ExportTask task) {
        return new ExportTaskResponse(
                task.exportId(),
                task.format(),
                task.status(),
                task.downloadPath(),
                task.createdAt(),
                task.completedAt(),
                task.expiresAt(),
                task.recordCount(),
                task.failureReason()
        );
    }
}
