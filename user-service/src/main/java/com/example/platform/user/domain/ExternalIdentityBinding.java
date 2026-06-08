package com.example.platform.user.domain;

import java.time.Instant;
import java.util.Map;

public record ExternalIdentityBinding(
        Long bindingId,
        String provider,
        String providerUserId,
        String providerUnionId,
        Long userId,
        String accountSnapshot,
        String userNameSnapshot,
        String emailSnapshot,
        String avatarSnapshot,
        Map<String, String> rawProfile,
        Instant createdAt,
        Instant updatedAt
) {
}
