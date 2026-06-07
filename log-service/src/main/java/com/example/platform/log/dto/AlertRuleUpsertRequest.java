package com.example.platform.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record AlertRuleUpsertRequest(
        @NotBlank(message = "ruleName is required") String ruleName,
        @NotBlank(message = "ruleType is required") String ruleType,
        String serviceName,
        @Positive(message = "threshold must be greater than 0") double threshold,
        @Positive(message = "windowMinutes must be greater than 0") int windowMinutes,
        boolean enabled,
        @NotNull(message = "notificationChannels is required") List<String> notificationChannels
) {
}
