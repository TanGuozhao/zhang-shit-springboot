package com.example.platform.user.dto;

import java.time.Instant;

public record UserProfileModificationRecordResponse(
        Instant modifyTime,
        String modifyField,
        String oldValue,
        String newValue
) {
}
