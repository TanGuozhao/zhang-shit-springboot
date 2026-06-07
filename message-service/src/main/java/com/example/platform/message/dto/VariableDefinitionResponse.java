package com.example.platform.message.dto;

public record VariableDefinitionResponse(
        String variableCode,
        String variableName,
        String description,
        String dataType,
        String defaultValue,
        boolean required,
        boolean enabled,
        boolean autoFill
) {
}
