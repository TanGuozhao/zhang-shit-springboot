package com.example.platform.topbiz.dto;

import com.example.platform.message.dto.MessageSendRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MessageAuditOrchestrationRequest(
        @Valid @NotNull(message = "message is required") MessageSendRequest message,
        String auditLevel,
        String auditSummary,
        Map<String, String> auditTags
) {
}
