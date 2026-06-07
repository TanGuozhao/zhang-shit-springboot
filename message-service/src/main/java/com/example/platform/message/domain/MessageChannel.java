package com.example.platform.message.domain;

public record MessageChannel(
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
