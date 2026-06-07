package com.example.platform.message.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageResponse(
        String messageId,
        String templateCode,
        String templateName,
        String channel,
        String status,
        String subject,
        String content,
        List<String> receivers,
        Map<String, String> variables,
        String dispatchType,
        Instant scheduledAt,
        String cronExpression,
        String batchCode,
        Instant createdAt,
        Instant updatedAt,
        Instant sentAt,
        List<String> attachmentIds
) {
}
