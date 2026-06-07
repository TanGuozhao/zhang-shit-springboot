package com.example.platform.message.domain;

import java.time.Instant;

public record DispatchTask(
        String taskId,
        String taskCode,
        String messageId,
        String receiver,
        String channelCode,
        String channelAccountCode,
        String schedulePolicyCode,
        String batchCode,
        Instant plannedAt,
        Instant actualAt,
        String status,
        int sortOrder,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
