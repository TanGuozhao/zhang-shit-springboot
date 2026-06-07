package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentAttributeDefinitionCreateRequest(
        @NotBlank(message = "attributeKey is required") String attributeKey,
        @NotBlank(message = "attributeName is required") String attributeName,
        @NotBlank(message = "dataType is required") String dataType,
        String defaultValue,
        Boolean required,
        String rules,
        Integer displayOrder
) {
}
