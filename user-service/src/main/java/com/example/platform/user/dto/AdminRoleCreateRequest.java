package com.example.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AdminRoleCreateRequest(
        @NotBlank(message = "roleCode is required") String roleCode,
        @NotBlank(message = "roleName is required") String roleName,
        String description,
        List<String> permissions
) {
}
