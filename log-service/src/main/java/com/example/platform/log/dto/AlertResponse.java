package com.example.platform.log.dto;

public record AlertResponse(
        String alertId,
        String alertCode,
        String level,
        String status,
        String summary
) {
}
