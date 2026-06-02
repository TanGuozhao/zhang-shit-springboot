package com.example.platform.message.domain;

import java.util.List;

public record MessageTemplate(
        String templateCode,
        String templateName,
        String channel,
        String content,
        boolean enabled,
        List<String> variables
) {
}
