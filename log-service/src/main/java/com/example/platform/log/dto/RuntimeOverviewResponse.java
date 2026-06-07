package com.example.platform.log.dto;

import com.example.platform.log.domain.RuntimeState;

import java.time.Instant;

public record RuntimeOverviewResponse(
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

    public static RuntimeOverviewResponse from(RuntimeState runtimeState) {
        return new RuntimeOverviewResponse(
                runtimeState.queueDepth(),
                runtimeState.queueCapacity(),
                runtimeState.schedulerEnabled(),
                runtimeState.lastFlushAt(),
                runtimeState.lastFlushResult(),
                runtimeState.lastAlertEvaluationAt(),
                runtimeState.lastAlertEvaluationResult(),
                runtimeState.lastExportRunAt(),
                runtimeState.lastExportRunResult(),
                runtimeState.lastCleanupAt(),
                runtimeState.lastCleanupResult(),
                runtimeState.pendingExports(),
                runtimeState.completedExports(),
                runtimeState.failedExports(),
                runtimeState.expiredExports(),
                runtimeState.archivedFailedBatches(),
                runtimeState.lastError()
        );
    }
}
