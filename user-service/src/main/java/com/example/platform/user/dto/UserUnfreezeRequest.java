package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUnfreezeRequest(
        String account,
        String contact,
        @NotBlank(message = "verifyCode is required") String verifyCode
) {
}
