package com.example.platform.log.domain;

public record AlertEvent(
        String alertId,
        String alertCode,
        String level,
        String status,
        String summary
) {
}
