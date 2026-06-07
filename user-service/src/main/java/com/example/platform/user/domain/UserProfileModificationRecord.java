package com.example.platform.user.domain;

import java.time.Instant;

public record UserProfileModificationRecord(
        Long recordId,
        Long userId,
        Instant modifyTime,
        String modifyField,
        String oldValue,
        String newValue
) {
}
