package com.example.platform.topbiz.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrchestrationExecutionRecord(
        String orchestrationId,
        String orchestrationType,
        String businessKey,
        String status,
        String traceId,
        Instant startedAt,
        Instant finishedAt,
        List<OrchestrationStepRecord> steps,
        Map<String, Object> result,
        String errorCode,
        String errorMessage
) {
}
