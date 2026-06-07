package com.example.platform.message.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageSendRequest(
        @NotBlank(message = "templateCode is required") String templateCode,
        @NotBlank(message = "channel is required") String channel,
        List<String> receivers,
        List<String> receiverGroups,
        Map<String, String> variables,
        String dispatchType,
        Instant scheduledAt,
        String cronExpression,
        String schedulePolicyCode,
        String channelAccountCode,
        List<String> attachmentIds,
        Boolean saveToInbox
) {
}
