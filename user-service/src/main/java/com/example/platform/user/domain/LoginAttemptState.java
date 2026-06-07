package com.example.platform.user.domain;

import java.time.Instant;

public record LoginAttemptState(
        String account,
        int failedCount,
        Instant lastFailedAt
) {
}
