package com.example.platform.user.dto;

public record RolePermissionDefinitionResponse(
        String permissionCode,
        String permissionName,
        String permissionType,
        String description
) {
}
