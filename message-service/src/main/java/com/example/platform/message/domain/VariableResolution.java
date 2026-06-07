package com.example.platform.message.domain;

import java.util.List;
import java.util.Map;

public record VariableResolution(
        MessageTemplate template,
        Map<String, String> resolvedVariables,
        List<String> errors,
        String renderedSubject,
        String renderedContent
) {
}
