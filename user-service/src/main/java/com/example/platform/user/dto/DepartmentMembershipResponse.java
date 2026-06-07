package com.example.platform.user.dto;

import java.util.Map;

public record DepartmentMembershipResponse(
        Long userId,
        Long departmentId,
        String departmentCode,
        String departmentName,
        String departmentPath,
        Map<String, String> memberAttributes
) {
}
