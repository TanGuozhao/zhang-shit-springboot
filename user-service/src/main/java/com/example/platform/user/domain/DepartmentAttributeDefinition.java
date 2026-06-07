package com.example.platform.user.domain;

import java.time.Instant;

public record DepartmentAttributeDefinition(
        Long attributeId,
        Long departmentId,
        String attributeKey,
        String attributeName,
        String dataType,
        String defaultValue,
        boolean required,
        String rules,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
