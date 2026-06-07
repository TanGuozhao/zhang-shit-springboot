package com.example.platform.message.dto;

import java.time.Instant;

public record ScheduleValidationRequest(
        String dispatchType,
        Instant scheduledAt,
        String cronExpression
) {
}
