package com.example.platform.message.domain;

public record CarrierAccount(
        String accountCode,
        String carrierName,
        String channelType,
        String apiKey,
        String endpoint,
        String signature,
        boolean enabled
) {
}
