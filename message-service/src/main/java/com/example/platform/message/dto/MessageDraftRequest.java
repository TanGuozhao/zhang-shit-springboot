package com.example.platform.message.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record MessageDraftRequest(
        @NotBlank(message = "templateCode is required") String templateCode,
        String title,
        String content,
        Map<String, String> variables
) {
}
