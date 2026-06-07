package com.example.platform.topbiz.dto;

import com.example.platform.topbiz.domain.OrchestrationStepRecord;

import java.time.Instant;
import java.util.Map;

public record OrchestrationStepResponse(
        String stepCode,
        String stepName,
        String status,
        String message,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> detail
) {

    public static OrchestrationStepResponse from(OrchestrationStepRecord record) {
        return new OrchestrationStepResponse(
                record.stepCode(),
                record.stepName(),
                record.status(),
                record.message(),
                record.startedAt(),
                record.finishedAt(),
                record.detail()
        );
    }
}
