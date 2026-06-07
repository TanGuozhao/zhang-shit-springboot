package com.example.platform.message.dto;

import java.util.Map;

public record MessageStatisticsResponse(
        long totalMessages,
        long successfulMessages,
        long failedMessages,
        long scheduledMessages,
        double successRate,
        Map<String, Long> channelBreakdown,
        Map<String, Long> failureReasons
) {
}
