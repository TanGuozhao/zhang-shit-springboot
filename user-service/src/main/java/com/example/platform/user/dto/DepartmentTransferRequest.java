package com.example.platform.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DepartmentTransferRequest(
        @NotEmpty(message = "userIds is required") List<Long> userIds,
        @NotNull(message = "fromDepartmentId is required") Long fromDepartmentId,
        @NotNull(message = "toDepartmentId is required") Long toDepartmentId
) {
}
