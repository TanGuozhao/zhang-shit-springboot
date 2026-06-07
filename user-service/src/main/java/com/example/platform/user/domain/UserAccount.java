package com.example.platform.user.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UserAccount(
        Long userId,
        String account,
        String password,
        String userName,
        String email,
        String phone,
        String avatar,
        String status,
        Long departmentId,
        List<String> roles,
        List<String> permissions,
        Map<String, String> extFields,
        Instant createdAt,
        Instant updatedAt
) {
}
