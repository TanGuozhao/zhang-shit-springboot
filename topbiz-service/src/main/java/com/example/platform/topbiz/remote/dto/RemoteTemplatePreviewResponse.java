package com.example.platform.topbiz.remote.dto;

import java.util.Map;

public record RemoteTemplatePreviewResponse(
        String templateCode,
        String renderedSubject,
        String renderedContent,
        Map<String, String> resolvedVariables
) {
}
