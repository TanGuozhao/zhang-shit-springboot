package com.example.platform.topbiz.remote.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RemoteMessageSendRequest(
        String templateCode,
        String channel,
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
