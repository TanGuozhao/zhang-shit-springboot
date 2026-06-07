package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record AuthLoginRequest(
        @NotBlank(message = "account is required") String account,
        String password,
        String loginType,
        Boolean rememberLogin,
        Map<String, String> thirdPartyInfo
) {
}
