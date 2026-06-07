package com.example.platform.user.domain;

import java.time.Instant;
import java.util.Map;

public record Department(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId,
        String description,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt
) {
}
