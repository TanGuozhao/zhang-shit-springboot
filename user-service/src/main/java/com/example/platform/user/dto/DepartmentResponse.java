package com.example.platform.user.dto;

public record DepartmentResponse(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId
) {
}
