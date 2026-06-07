package com.example.platform.message.domain;

import java.time.Instant;

public record InboxMessage(
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
