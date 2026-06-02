package com.example.platform.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UserAuthorizationUpdateRequest(
        @NotEmpty(message = "roles are required") List<String> roles,
        @NotEmpty(message = "permissions are required") List<String> permissions
) {
}
