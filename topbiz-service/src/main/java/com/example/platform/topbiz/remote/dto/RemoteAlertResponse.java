package com.example.platform.topbiz.remote.dto;

public record RemoteAlertResponse(
        String alertId,
        String alertCode,
        String level,
        String status,
        String summary
) {
}
