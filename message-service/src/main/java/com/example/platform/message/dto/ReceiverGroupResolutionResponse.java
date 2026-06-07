package com.example.platform.message.dto;

import java.util.List;

public record ReceiverGroupResolutionResponse(
        String groupCode,
        List<String> receivers
) {
}
