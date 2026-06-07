package com.example.platform.message.dto;

public record CarrierAccountResponse(
        String accountCode,
        String carrierName,
        String channelType,
        String endpoint,
        String signature,
        boolean enabled
) {
}
