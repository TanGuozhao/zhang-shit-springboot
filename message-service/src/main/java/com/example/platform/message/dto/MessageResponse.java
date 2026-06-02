package com.example.platform.message.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageResponse(
        String messageId,
        String templateCode,
        String channel,
        String status,
        List<String> receivers,
        Map<String, String> variables,
        Instant createdAt
) {
}
