package com.example.platform.message.dto;

public record SchedulePolicyResponse(
        String policyCode,
        String cronExpression,
        String policyType,
        boolean enabled,
        String description
) {
}
