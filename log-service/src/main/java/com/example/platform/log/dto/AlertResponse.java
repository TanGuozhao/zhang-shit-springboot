package com.example.platform.log.dto;

import com.example.platform.log.domain.AlertEvent;

public record AlertResponse(
        String alertId,
        String alertCode,
        String level,
        String status,
        String summary
) {

    public static AlertResponse from(AlertEvent alertEvent) {
        return new AlertResponse(
                alertEvent.alertId(),
                alertEvent.alertCode(),
                alertEvent.level(),
                alertEvent.status(),
                alertEvent.summary()
        );
    }
}
