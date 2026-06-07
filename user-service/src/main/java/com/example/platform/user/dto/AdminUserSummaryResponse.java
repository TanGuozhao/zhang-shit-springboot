package com.example.platform.user.dto;

import java.time.Instant;

public record AdminUserSummaryResponse(
        Long userId,
        String account,
        String userName,
        String status,
        Long departmentId,
        Instant createTime,
        Instant updateTime
) {
}
