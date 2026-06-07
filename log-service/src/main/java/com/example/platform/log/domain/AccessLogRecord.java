package com.example.platform.log.domain;

import java.time.Instant;
import java.util.Map;

public record AccessLogRecord(
        String logId,
        String serviceName,
        String traceId,
        String level,
        String message,
        String path,
        Integer statusCode,
        Long latencyMs,
        String requestId,
        String clientIp,
        Instant timestamp,
        Map<String, String> tags
) {
}
