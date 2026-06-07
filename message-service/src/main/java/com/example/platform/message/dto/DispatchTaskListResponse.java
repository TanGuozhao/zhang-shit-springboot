package com.example.platform.message.dto;

import java.util.List;

public record DispatchTaskListResponse(
        List<DispatchTaskResponse> records,
        long total
) {
}
