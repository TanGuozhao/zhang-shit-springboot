package com.example.platform.topbiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topbiz.remote")
public record TopbizRemoteProperties(
        ServiceEndpoint userService,
        ServiceEndpoint messageService,
        ServiceEndpoint logService
) {
    public record ServiceEndpoint(String baseUrl) {
    }
}
