package com.example.platform.log.dto;

import com.example.platform.log.domain.AlertRule;

import java.util.List;

public record AlertRuleResponse(
        String ruleId,
        String ruleName,
        String ruleType,
        String serviceName,
        double threshold,
        int windowMinutes,
        boolean enabled,
        List<String> notificationChannels
) {

    public static AlertRuleResponse from(AlertRule rule) {
        return new AlertRuleResponse(
                rule.ruleId(),
                rule.ruleName(),
                rule.ruleType().name(),
                rule.serviceName(),
                rule.threshold(),
                rule.windowMinutes(),
                rule.enabled(),
                rule.notificationChannels()
        );
    }
}
