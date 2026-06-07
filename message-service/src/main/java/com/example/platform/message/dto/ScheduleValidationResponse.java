package com.example.platform.message.dto;

import java.time.Instant;

public record ScheduleValidationResponse(
        boolean valid,
        String dispatchType,
        Instant scheduledAt,
        String cronExpression,
        String message
) {
}
