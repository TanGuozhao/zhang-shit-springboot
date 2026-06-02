package com.example.platform.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record MessageSendRequest(
        @NotBlank(message = "templateCode is required") String templateCode,
        @NotBlank(message = "channel is required") String channel,
        @NotEmpty(message = "receivers are required") List<String> receivers,
        Map<String, String> variables
) {
}
