package com.example.platform.topbiz.remote.dto;

import java.util.Map;

public record RemoteMetricsResponse(
        String serviceName,
        Map<String, Number> metrics
) {
}
