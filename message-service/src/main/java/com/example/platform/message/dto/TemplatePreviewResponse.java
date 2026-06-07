package com.example.platform.message.dto;

import java.util.Map;

public record TemplatePreviewResponse(
        String templateCode,
        String renderedSubject,
        String renderedContent,
        Map<String, String> resolvedVariables
) {
}
