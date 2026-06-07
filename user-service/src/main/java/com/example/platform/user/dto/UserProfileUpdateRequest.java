package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record UserProfileUpdateRequest(
        @NotBlank(message = "password is required") String password,
        String userName,
        String contact,
        String avatar,
        Map<String, String> extFields
) {
}
