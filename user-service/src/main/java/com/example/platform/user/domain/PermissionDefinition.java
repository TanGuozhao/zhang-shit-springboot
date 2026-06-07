package com.example.platform.user.domain;

public record PermissionDefinition(
        String permissionCode,
        String permissionName,
        String permissionType,
        String description
) {
}
