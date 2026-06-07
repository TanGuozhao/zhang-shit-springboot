package com.example.platform.user.dto;

import java.time.Instant;
import java.util.List;

public record AuthLoginResponse(
        Long userId,
        String account,
        String userName,
        List<String> roles,
        List<String> permissions,
        String sessionKey,
        Instant expireTime
) {
}
