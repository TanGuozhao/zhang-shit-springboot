package com.example.platform.log.domain;

import java.time.Instant;

public record RuntimeState(
        int queueDepth,
        int queueCapacity,
        boolean schedulerEnabled,
        Instant lastFlushAt,
        String lastFlushResult,
        Instant lastAlertEvaluationAt,
        String lastAlertEvaluationResult,
        Instant lastExportRunAt,
        String lastExportRunResult,
        Instant lastCleanupAt,
        String lastCleanupResult,
        int pendingExports,
        int completedExports,
        int failedExports,
        int expiredExports,
        int archivedFailedBatches,
        String lastError
) {
}
