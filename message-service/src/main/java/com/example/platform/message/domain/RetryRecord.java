package com.example.platform.message.domain;

import java.time.Instant;

public record RetryRecord(
        String retryId,
        String messageId,
        String dispatchTaskId,
        int retryCount,
        String retryStatus,
        Instant lastRetryAt,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
