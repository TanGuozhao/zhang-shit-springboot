package com.example.platform.user.domain;

import java.time.Instant;

public record UserSession(
        String sessionKey,
        Long userId,
        Instant issuedAt,
        Instant expiresAt
) {
}
