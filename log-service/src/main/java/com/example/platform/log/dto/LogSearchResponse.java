package com.example.platform.log.dto;

import java.util.List;

public record LogSearchResponse(
        List<LogEntryResponse> records,
        long total
) {
}
