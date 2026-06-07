package com.example.platform.message.dto;

import java.time.Instant;

public record DispatchTaskTimeUpdateRequest(
        Instant plannedAt
) {
}
