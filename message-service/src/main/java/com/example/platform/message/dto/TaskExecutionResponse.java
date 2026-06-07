package com.example.platform.message.dto;

import java.time.Instant;

public record TaskExecutionResponse(
        int processedCount,
        int successCount,
        int failureCount,
        int skippedCount,
        Instant runAt
) {
}
