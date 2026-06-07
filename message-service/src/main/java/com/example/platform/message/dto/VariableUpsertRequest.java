package com.example.platform.message.dto;

public record VariableUpsertRequest(
        String variableCode,
        String variableName,
        String description,
        String dataType,
        String defaultValue,
        boolean required,
        Boolean enabled,
        Boolean autoFill
) {
}
