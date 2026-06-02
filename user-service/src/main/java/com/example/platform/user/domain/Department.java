package com.example.platform.user.domain;

public record Department(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId
) {
}
