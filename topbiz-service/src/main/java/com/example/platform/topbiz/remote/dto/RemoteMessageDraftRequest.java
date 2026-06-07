package com.example.platform.topbiz.remote.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RemoteMessageDraftRequest(
        String templateCode,
        String channel,
        List<String> receivers,
        List<String> receiverGroups,
        String title,
        String content,
        Map<String, String> variables,
        Instant scheduledAt,
        String cronExpression,
        List<String> attachmentIds
) {
}
