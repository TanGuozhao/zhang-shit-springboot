package com.example.platform.message.dto;

import java.time.Instant;

public record DispatchTaskRequest(
        String taskCode,
        String messageId,
        String channelCode,
        String channelAccountCode,
        String schedulePolicyCode,
        Instant plannedAt,
        Integer sortOrder
) {
}
