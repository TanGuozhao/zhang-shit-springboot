package com.example.platform.message.dto;

public record SchedulePolicyRequest(
        String policyCode,
        String cronExpression,
        String policyType,
        Boolean enabled,
        String description
) {
}
