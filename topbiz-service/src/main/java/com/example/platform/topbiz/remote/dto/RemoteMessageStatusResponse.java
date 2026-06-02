package com.example.platform.topbiz.remote.dto;

import java.time.Instant;

public record RemoteMessageStatusResponse(
        String messageId,
        String status,
        Instant updatedAt
) {
}
