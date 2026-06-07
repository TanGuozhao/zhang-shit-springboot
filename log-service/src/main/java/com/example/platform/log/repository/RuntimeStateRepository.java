package com.example.platform.log.repository;

import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.RuntimeState;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class RuntimeStateRepository {

    private final LogServiceProperties properties;
    private Instant lastFlushAt;
    private String lastFlushResult = "NOT_RUN";
    private Instant lastAlertEvaluationAt;
    private String lastAlertEvaluationResult = "NOT_RUN";
    private Instant lastExportRunAt;
    private String lastExportRunResult = "NOT_RUN";
    private Instant lastCleanupAt;
    private String lastCleanupResult = "NOT_RUN";
    private int archivedFailedBatches;
    private String lastError;

    public RuntimeStateRepository(LogServiceProperties properties) {
        this.properties = properties;
    }

    public synchronized void markFlush(Instant timestamp, String result, String error) {
        this.lastFlushAt = timestamp;
        this.lastFlushResult = result;
        this.lastError = error;
    }

    public synchronized void markAlertEvaluation(Instant timestamp, String result, String error) {
        this.lastAlertEvaluationAt = timestamp;
        this.lastAlertEvaluationResult = result;
        this.lastError = error;
    }

    public synchronized void markExportRun(Instant timestamp, String result, String error) {
        this.lastExportRunAt = timestamp;
        this.lastExportRunResult = result;
        this.lastError = error;
    }

    public synchronized void markCleanup(Instant timestamp, String result, String error) {
        this.lastCleanupAt = timestamp;
        this.lastCleanupResult = result;
        this.lastError = error;
    }

    public synchronized void incrementArchivedFailedBatches(int count) {
        this.archivedFailedBatches += count;
    }

    public synchronized void markError(String error) {
        this.lastError = error;
    }

    public synchronized RuntimeState snapshot(int queueDepth,
                                              int pendingExports,
                                              int completedExports,
                                              int failedExports,
                                              int expiredExports) {
        return new RuntimeState(
                queueDepth,
                properties.buffer().capacity(),
                properties.scheduler().enabled(),
                lastFlushAt,
                lastFlushResult,
                lastAlertEvaluationAt,
                lastAlertEvaluationResult,
                lastExportRunAt,
                lastExportRunResult,
                lastCleanupAt,
                lastCleanupResult,
                pendingExports,
                completedExports,
                failedExports,
                expiredExports,
                archivedFailedBatches,
                lastError
        );
    }
}
