package com.example.platform.user.dto;

import java.util.List;

public record AdminRolePermissionAllocateRequest(
        List<String> permissions
) {
}
