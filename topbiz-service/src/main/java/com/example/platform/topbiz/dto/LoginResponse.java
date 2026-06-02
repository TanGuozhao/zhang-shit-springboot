package com.example.platform.topbiz.dto;

import java.util.List;

public record LoginResponse(
        String sessionId,
        Long userId,
        String account,
        String userName,
        List<String> roles,
        List<String> permissions
) {
}
