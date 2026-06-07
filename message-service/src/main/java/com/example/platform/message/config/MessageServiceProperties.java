package com.example.platform.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.message")
public record MessageServiceProperties(
        String boundedContext,
        String publicBasePath,
        boolean schedulerEnabled,
        boolean retryEnabled,
        int maxRetryAttempts
) {
}
