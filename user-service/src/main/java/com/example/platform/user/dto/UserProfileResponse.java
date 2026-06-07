package com.example.platform.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UserProfileResponse(
        Long userId,
        String account,
        String userName,
        String email,
        String phone,
        String avatar,
        String status,
        String statusDesc,
        Long departmentId,
        List<String> roles,
        List<String> permissions,
        Map<String, String> extFields,
        Instant createTime,
        Instant updateTime
) {
}
