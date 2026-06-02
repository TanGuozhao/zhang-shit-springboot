package com.example.platform.topbiz.remote.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RemoteMessageResponse(
        String messageId,
        String templateCode,
        String channel,
        String status,
        List<String> receivers,
        Map<String, String> variables,
        Instant createdAt
) {
}
