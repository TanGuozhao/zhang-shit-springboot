package com.example.platform.user.dto;

import java.time.Instant;
import java.util.Map;

public record AdminDepartmentSummaryResponse(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId,
        String description,
        Map<String, String> attributes,
        Instant createTime,
        Instant updateTime
) {
}
