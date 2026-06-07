package com.example.platform.log.domain;

import java.time.Instant;

public record LogSearchCriteria(
        String keyword,
        String serviceName,
        String level,
        String traceId,
        Integer statusCode,
        Instant startTime,
        Instant endTime,
        int page,
        int size
) {

    public static LogSearchCriteria keywordOnly(String keyword) {
        return new LogSearchCriteria(keyword, null, null, null, null, null, null, 0, 1000);
    }

    public static LogSearchCriteria trace(String traceId) {
        return new LogSearchCriteria(null, null, null, traceId, null, null, null, 0, 1000);
    }
}
