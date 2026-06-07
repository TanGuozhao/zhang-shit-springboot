package com.example.platform.log.domain;

import java.time.Instant;

public record AlertEvent(
        String alertId,
        String alertCode,
        String level,
        String status,
        String summary,
        String ruleId,
        Instant createdAt
) {
}
