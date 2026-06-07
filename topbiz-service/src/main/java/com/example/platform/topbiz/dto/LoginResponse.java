package com.example.platform.topbiz.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String sessionId,
        String sessionKey,
        Instant expireTime,
        Long userId,
        String account,
        String userName,
        List<String> roles,
        List<String> permissions
) {
}
