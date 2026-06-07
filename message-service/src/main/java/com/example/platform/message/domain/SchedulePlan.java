package com.example.platform.message.domain;

import java.time.Instant;

public record SchedulePlan(
        String dispatchType,
        Instant scheduledAt,
        String cronExpression,
        String description
) {
}
