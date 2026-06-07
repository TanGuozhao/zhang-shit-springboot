package com.example.platform.message.dto;

public record CarrierAccountRequest(
        String carrierName,
        String channelType,
        String accountCode,
        String apiKey,
        String endpoint,
        String signature,
        Boolean enabled
) {
}
