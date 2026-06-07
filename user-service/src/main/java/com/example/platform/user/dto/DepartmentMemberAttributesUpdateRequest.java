package com.example.platform.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DepartmentMemberAttributesUpdateRequest(
        @NotNull(message = "attributes is required") Map<String, String> attributes
) {
}
