package com.example.platform.topbiz.domain;

import java.time.Instant;
import java.util.Map;

public record OrchestrationStepRecord(
        String stepCode,
        String stepName,
        String status,
        String message,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> detail
) {
}
