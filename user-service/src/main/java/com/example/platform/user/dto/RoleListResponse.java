package com.example.platform.user.dto;

import java.util.List;

public record RoleListResponse(
        Long userId,
        List<String> roles
) {
}
