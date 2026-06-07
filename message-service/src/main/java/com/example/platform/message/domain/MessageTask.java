package com.example.platform.message.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageTask(
        String messageId,
        String templateCode,
        String templateName,
        String channel,
        String channelAccountCode,
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
        String errorCode,
        String errorReason,
        int retryCount,
        List<String> attachmentIds
) {
}
