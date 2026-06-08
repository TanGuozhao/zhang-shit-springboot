package com.example.platform.topbiz.service;

import java.util.Map;

public record TopbizOAuthProviderProfile(
        String provider,
        String providerUserId,
        String providerUnionId,
        String account,
        String email,
        String phone,
        String userName,
        String avatar,
        Map<String, String> rawProfile
) {
}
