package com.example.platform.user.domain;

import java.time.Instant;
import java.util.List;

public record RoleDefinition(
        Long roleId,
        String roleCode,
        String roleName,
        String description,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
