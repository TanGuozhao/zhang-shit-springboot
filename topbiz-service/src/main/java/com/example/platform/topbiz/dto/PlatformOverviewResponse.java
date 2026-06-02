package com.example.platform.topbiz.dto;

import java.util.Map;

public record PlatformOverviewResponse(
        Long currentUserId,
        String currentUserName,
        Map<String, Object> user,
        Map<String, Object> message,
        Map<String, Object> log
) {
}
