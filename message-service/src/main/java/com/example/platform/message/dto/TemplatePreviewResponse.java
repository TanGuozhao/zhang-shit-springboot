package com.example.platform.message.dto;

public record TemplatePreviewResponse(
        String templateCode,
        String renderedContent
) {
}
