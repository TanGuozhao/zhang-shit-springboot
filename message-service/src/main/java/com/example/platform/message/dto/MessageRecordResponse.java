package com.example.platform.message.dto;

import java.util.List;

public record MessageRecordResponse(
        List<MessageResponse> records,
        long total
) {
}
