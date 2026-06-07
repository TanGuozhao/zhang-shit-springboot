package com.example.platform.message.dto;

import java.util.List;

public record ReceiverResolutionRequest(
        List<String> receivers,
        List<String> receiverGroups
) {
}
