package com.example.platform.topbiz.remote.dto;

public record RemoteDepartmentResponse(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId
) {
}
