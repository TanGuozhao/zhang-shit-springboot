package com.example.platform.message.dto;

import java.time.Instant;

public record MessageStatusResponse(
        String messageId,
        String status,
        Instant updatedAt
) {
}
