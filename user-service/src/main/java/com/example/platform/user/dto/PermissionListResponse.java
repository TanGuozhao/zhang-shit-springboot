package com.example.platform.user.dto;

import java.util.List;

public record PermissionListResponse(
        Long userId,
        List<String> permissions
) {
}
