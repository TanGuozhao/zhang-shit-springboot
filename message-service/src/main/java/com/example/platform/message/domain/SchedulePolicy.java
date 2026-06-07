package com.example.platform.message.domain;

import java.time.Instant;

public record SchedulePolicy(
        String policyCode,
        String cronExpression,
        String policyType,
        boolean enabled,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
