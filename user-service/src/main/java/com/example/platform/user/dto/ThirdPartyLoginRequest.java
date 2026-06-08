package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ThirdPartyLoginRequest(
        @NotBlank(message = "provider is required")
        String provider,
        @NotBlank(message = "providerUserId is required")
        String providerUserId,
        String providerUnionId,
        String account,
        String email,
        String phone,
        String userName,
        String avatar,
        Map<String, String> rawProfile,
        Boolean autoRegister
) {
}
