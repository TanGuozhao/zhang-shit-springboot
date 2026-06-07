package com.example.platform.message.domain;

public record MessageVariable(
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
