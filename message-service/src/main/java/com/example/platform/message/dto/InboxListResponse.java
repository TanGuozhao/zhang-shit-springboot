package com.example.platform.message.dto;

import java.util.List;

public record InboxListResponse(
        List<InboxMessageResponse> records,
        long total
) {
}
