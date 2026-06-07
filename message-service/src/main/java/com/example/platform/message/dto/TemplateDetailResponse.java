package com.example.platform.message.dto;

import java.util.List;

public record TemplateDetailResponse(
        String templateCode,
        String templateName,
        String channel,
        String subjectTemplate,
        String contentTemplate,
        String description,
        boolean enabled,
        List<VariableDefinitionResponse> variables
) {
}
