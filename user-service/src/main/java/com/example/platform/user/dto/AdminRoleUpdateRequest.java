package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AdminRoleUpdateRequest(
        @NotBlank(message = "roleName is required") String roleName,
        String description,
        List<String> permissions
) {
}
