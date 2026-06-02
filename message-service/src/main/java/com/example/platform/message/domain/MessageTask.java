package com.example.platform.message.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageTask(
        String messageId,
        String templateCode,
        String channel,
        String status,
        List<String> receivers,
        Map<String, String> variables,
        Instant createdAt
) {
}
