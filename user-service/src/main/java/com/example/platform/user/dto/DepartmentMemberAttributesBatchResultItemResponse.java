package com.example.platform.user.dto;

import java.util.Map;

public record DepartmentMemberAttributesBatchResultItemResponse(
        Long userId,
        boolean success,
        String message,
        Map<String, String> attributes
) {
}
