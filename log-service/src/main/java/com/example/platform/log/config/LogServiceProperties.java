package com.example.platform.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.log")
public record LogServiceProperties(
        String boundedContext,
        String publicBasePath
) {
}
