package com.example.platform.message.dto;

import java.util.List;

public record ReceiverResolutionResponse(
        List<String> receivers,
        List<ReceiverGroupResolutionResponse> groups
) {
}
