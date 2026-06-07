package com.example.platform.message.dto;

public record TemplateSummaryResponse(
        String templateCode,
        String templateName,
        String channel,
        String description,
        boolean enabled
) {
}
