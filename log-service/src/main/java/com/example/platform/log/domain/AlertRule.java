package com.example.platform.log.domain;

import java.util.List;

public record AlertRule(
        String ruleId,
        String ruleName,
        AlertRuleType ruleType,
        String serviceName,
        double threshold,
        int windowMinutes,
        boolean enabled,
        List<String> notificationChannels
) {
}
