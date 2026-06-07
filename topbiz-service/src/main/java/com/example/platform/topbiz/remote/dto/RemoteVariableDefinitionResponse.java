package com.example.platform.topbiz.remote.dto;

public record RemoteVariableDefinitionResponse(
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
