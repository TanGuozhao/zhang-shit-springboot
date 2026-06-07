package com.example.platform.message.domain;

import java.util.List;

public record MessageTemplate(
        String templateCode,
        String templateName,
        String channel,
        String subjectTemplate,
        String contentTemplate,
        String description,
        boolean enabled,
        List<String> variableCodes
) {
}
