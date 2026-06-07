package com.example.platform.user.dto;

import java.time.Instant;

public record VerifyCodeSendResponse(
        String account,
        String contact,
        String scene,
        Instant expireTime
) {
}
