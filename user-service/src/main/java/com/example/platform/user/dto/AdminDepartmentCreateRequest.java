package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AdminDepartmentCreateRequest(
        @NotBlank(message = "departmentCode is required") String departmentCode,
        @NotBlank(message = "departmentName is required") String departmentName,
        @NotNull(message = "parentDepartmentId is required") Long parentDepartmentId,
        String description,
        Map<String, String> attributes
) {
}
