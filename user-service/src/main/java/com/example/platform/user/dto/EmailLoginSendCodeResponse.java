package com.example.platform.user.dto;

import java.time.Instant;

public record EmailLoginSendCodeResponse(
        String email,
        Instant expireTime
) {
}
