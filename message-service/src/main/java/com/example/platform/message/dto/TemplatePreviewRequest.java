package com.example.platform.message.dto;

import java.util.Map;

public record TemplatePreviewRequest(
        Map<String, String> variables
) {
}
