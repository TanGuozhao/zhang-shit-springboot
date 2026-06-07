package com.example.platform.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.user")
public record UserServiceProperties(
        String boundedContext,
        String publicBasePath,
        Integer sessionTtlHours,
        Integer passwordMinLength,
        Integer passwordHistoryLimit,
        Integer loginFailureFreezeThreshold,
        Integer verifyCodeTtlMinutes,
        Integer verifyCodeCooldownSeconds
) {
}
