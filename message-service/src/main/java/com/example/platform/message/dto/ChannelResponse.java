package com.example.platform.message.dto;

public record ChannelResponse(
        String channelCode,
        String channelType,
        String carrierName,
        String accountCode,
        String sender,
        boolean enabled,
        boolean healthy,
        String description
) {
}
