package com.example.platform.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record InternalLogSearchRequest(
        String keyword,
        String serviceName,
        String level,
        String traceId,
        Integer statusCode,
        Instant startTime,
        Instant endTime,
        @Min(value = 0, message = "page must be greater than or equal to 0") Integer page,
        @Min(value = 1, message = "size must be greater than 0")
        @Max(value = 500, message = "size must be less than or equal to 500") Integer size
) {
}
