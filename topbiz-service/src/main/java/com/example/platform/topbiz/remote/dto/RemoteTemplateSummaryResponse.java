package com.example.platform.topbiz.remote.dto;

public record RemoteTemplateSummaryResponse(
        String templateCode,
        String templateName,
        String channel,
        boolean enabled
) {
}
