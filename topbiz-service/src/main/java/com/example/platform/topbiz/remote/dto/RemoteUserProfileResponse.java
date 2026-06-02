package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemoteUserProfileResponse(
        Long userId,
        String account,
        String userName,
        String email,
        String phone,
        String status,
        Long departmentId,
        List<String> roles,
        List<String> permissions
) {
}
