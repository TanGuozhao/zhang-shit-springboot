package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemoteLogSearchResponse(
        List<RemoteLogEntryResponse> records,
        long total
) {
}
