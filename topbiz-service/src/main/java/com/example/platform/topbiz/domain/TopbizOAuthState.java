package com.example.platform.topbiz.domain;

import java.time.Instant;

public record TopbizOAuthState(
        String provider,
        String state,
        Instant issuedAt,
        Instant expiresAt
) {
}
