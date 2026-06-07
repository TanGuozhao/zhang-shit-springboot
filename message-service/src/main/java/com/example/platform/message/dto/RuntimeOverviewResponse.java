package com.example.platform.message.dto;

import java.time.Instant;

public record RuntimeOverviewResponse(
        long pendingDispatchTasks,
        long processingDispatchTasks,
        long pendingRetryRecords,
        long inboxMessages,
        boolean schedulerEnabled,
        boolean retryEnabled,
        Instant lastDispatchRunAt,
        Instant lastRetryRunAt
) {
}
