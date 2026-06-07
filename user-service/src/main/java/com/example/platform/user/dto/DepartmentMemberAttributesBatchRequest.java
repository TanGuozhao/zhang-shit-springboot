package com.example.platform.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DepartmentMemberAttributesBatchRequest(
        @NotEmpty(message = "operations is required") List<DepartmentMemberAttributesBatchItemRequest> operations
) {
}
