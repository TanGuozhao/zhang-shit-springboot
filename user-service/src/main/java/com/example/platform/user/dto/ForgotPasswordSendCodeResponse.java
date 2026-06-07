package com.example.platform.user.dto;

import java.time.Instant;

public record ForgotPasswordSendCodeResponse(
        String account,
        String contact,
        Instant expireTime
) {
}
