package com.example.platform.message.dto;

import java.util.List;
import java.util.Map;

public record VariableValidationResponse(
        String templateCode,
        boolean valid,
        Map<String, String> resolvedVariables,
        List<String> errors
) {
}
