package com.example.platform.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DepartmentMemberAttributesBatchItemRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "attributes is required") Map<String, String> attributes
) {
}
