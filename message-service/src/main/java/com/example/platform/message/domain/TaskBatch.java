package com.example.platform.message.domain;

import java.time.Instant;

public record TaskBatch(
        String batchCode,
        int totalTaskCount,
        int processedTaskCount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
