package com.example.platform.message.dto;

import java.time.Instant;

public record DispatchTaskResponse(
        String taskId,
        String taskCode,
        String messageId,
        String receiver,
        String channelCode,
        Instant plannedAt,
        Instant actualAt,
        String status,
        int sortOrder,
        String lastError
) {
}
