package com.example.platform.log.domain;

public record FlushReport(
        boolean success,
        int processedCount,
        String message
) {
}
