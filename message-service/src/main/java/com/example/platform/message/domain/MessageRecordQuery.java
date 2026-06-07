package com.example.platform.message.domain;

import java.time.Instant;

public record MessageRecordQuery(
        String status,
        String channel,
        String keyword,
        String receiver,
        Instant startTime,
        Instant endTime
) {
}
