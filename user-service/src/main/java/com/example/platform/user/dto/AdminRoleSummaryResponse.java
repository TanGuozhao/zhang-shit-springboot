package com.example.platform.user.dto;

import java.time.Instant;
import java.util.List;

public record AdminRoleSummaryResponse(
        Long roleId,
        String roleCode,
        String roleName,
        String description,
        List<String> permissions,
        Instant createTime,
        Instant updateTime
) {
}
