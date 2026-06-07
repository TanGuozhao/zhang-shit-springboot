package com.example.platform.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DepartmentMemberRelationRequest(
        @NotEmpty(message = "userIds is required") List<Long> userIds
) {
}
