package com.example.platform.message.dto;

import java.time.Instant;

public record MessageStatusResponse(
        String messageId,
        String status,
        String dispatchType,
        Instant scheduledAt,
        Instant sentAt,
        Instant updatedAt
) {
}
