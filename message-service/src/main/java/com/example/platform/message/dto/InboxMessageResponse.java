package com.example.platform.message.dto;

import java.time.Instant;

public record InboxMessageResponse(
        String inboxId,
        String messageId,
        String receiver,
        String channel,
        String subject,
        String content,
        String readStatus,
        Instant deliveredAt,
        Instant readAt
) {
}
