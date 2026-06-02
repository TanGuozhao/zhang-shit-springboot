package com.example.platform.user.domain;

import java.util.List;

public record UserAccount(
        Long userId,
        String account,
        String password,
        String userName,
        String email,
        String phone,
        String status,
        Long departmentId,
        List<String> roles,
        List<String> permissions
) {
}
