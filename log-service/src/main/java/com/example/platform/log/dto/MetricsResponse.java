package com.example.platform.log.dto;

import java.util.Map;

public record MetricsResponse(
        String serviceName,
        Map<String, Number> metrics
) {
}
