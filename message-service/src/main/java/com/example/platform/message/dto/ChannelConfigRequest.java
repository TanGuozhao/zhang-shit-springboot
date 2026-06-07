package com.example.platform.message.dto;

public record ChannelConfigRequest(
        String channelCode,
        String channelType,
        String carrierName,
        String accountCode,
        String sender,
        Boolean enabled,
        Boolean healthy,
        String description
) {
}
