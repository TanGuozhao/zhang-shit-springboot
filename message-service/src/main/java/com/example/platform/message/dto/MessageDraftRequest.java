package com.example.platform.message.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageDraftRequest(
        @NotBlank(message = "templateCode is required") String templateCode,
        String channel,
        List<String> receivers,
        List<String> receiverGroups,
        String title,
        String content,
        Map<String, String> variables,
        Instant scheduledAt,
        String cronExpression,
        List<String> attachmentIds
) {
}
