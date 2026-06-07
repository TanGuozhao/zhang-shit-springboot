package com.example.platform.user.dto;

import java.time.Instant;

public record DepartmentAttributeDefinitionResponse(
        Long attributeId,
        Long departmentId,
        String attributeKey,
        String attributeName,
        String dataType,
        String defaultValue,
        boolean required,
        String rules,
        Integer displayOrder,
        Instant createTime,
        Instant updateTime
) {
}
