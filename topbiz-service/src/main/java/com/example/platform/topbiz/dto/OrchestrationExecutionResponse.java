package com.example.platform.topbiz.dto;

import com.example.platform.topbiz.domain.OrchestrationExecutionRecord;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrchestrationExecutionResponse(
        String orchestrationId,
        String orchestrationType,
        String businessKey,
        String status,
        String traceId,
        Instant startedAt,
        Instant finishedAt,
        List<OrchestrationStepResponse> steps,
        Map<String, Object> result,
        String errorCode,
        String errorMessage
) {

    public static OrchestrationExecutionResponse from(OrchestrationExecutionRecord record) {
        return new OrchestrationExecutionResponse(
                record.orchestrationId(),
                record.orchestrationType(),
                record.businessKey(),
                record.status(),
                record.traceId(),
                record.startedAt(),
                record.finishedAt(),
                record.steps().stream().map(OrchestrationStepResponse::from).toList(),
                record.result(),
                record.errorCode(),
                record.errorMessage()
        );
    }
}
