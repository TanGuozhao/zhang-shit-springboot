package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UserRegistrationRequest(
        @NotBlank(message = "account is required") String account,
        @NotBlank(message = "password is required") String password,
        @NotBlank(message = "userName is required") String userName,
        @NotBlank(message = "contact is required") String contact,
        @NotBlank(message = "verifyCode is required") String verifyCode,
        @NotNull(message = "agreeProtocol is required") Boolean agreeProtocol,
        Map<String, String> extFields
) {
}
