package com.example.platform.message.dto;

import java.time.Instant;

public record MessageErrorResponse(
        String messageId,
        String errorCode,
        String errorReason,
        boolean retryable,
        int retryCount,
        Instant updatedAt
) {
}
