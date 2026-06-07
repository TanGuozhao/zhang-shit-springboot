package com.example.platform.message.dto;

import java.util.List;

public record TemplateUpsertRequest(
        String templateCode,
        String templateName,
        String channel,
        String subjectTemplate,
        String contentTemplate,
        String description,
        List<String> variableCodes,
        Boolean enabled
) {
}
