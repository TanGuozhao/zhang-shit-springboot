package com.example.platform.topbiz.dto;

import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.dto.UserProfileResponse;

import java.util.Map;

public record PlatformOverviewResponse(
        LoginResponse session,
        UserProfileResponse currentUser,
        DepartmentResponse currentDepartment,
        PlatformArchitectureResponse architecture,
        PlatformRuntimeResponse runtime,
        Map<String, Object> quickView
) {
}
