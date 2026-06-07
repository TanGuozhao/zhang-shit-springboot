package com.example.platform.user.dto;

public record DepartmentUserSummaryResponse(
        Long userId,
        String account,
        String userName,
        String status
) {
}
