package com.example.platform.message.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record VariableFillRequest(
        @NotBlank(message = "templateCode is required") String templateCode,
        Map<String, String> variables
) {
}
