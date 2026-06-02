package com.example.platform.user.dto;

import java.util.List;

public record UserProfileResponse(
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
