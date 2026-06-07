package com.example.platform.user.dto;

public record UserRegistrationResponse(
        Long userId,
        String account,
        String status
) {
}
