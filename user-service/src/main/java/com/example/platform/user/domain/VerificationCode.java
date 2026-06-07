package com.example.platform.user.domain;

import java.time.Instant;

public record VerificationCode(
        String account,
        String contact,
        String scene,
        String code,
        Instant issuedAt,
        Instant expiresAt
) {
}
