package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemoteTemplateDetailResponse(
        String templateCode,
        String templateName,
        String channel,
        String content,
        List<String> variables
) {
}
