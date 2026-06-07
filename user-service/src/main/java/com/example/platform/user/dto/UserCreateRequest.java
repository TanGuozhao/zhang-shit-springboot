package com.example.platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record UserCreateRequest(
        @NotBlank(message = "account is required") String account,
        @NotBlank(message = "password is required") String password,
        @NotBlank(message = "userName is required") String userName,
        @Email(message = "email must be valid") String email,
        @NotBlank(message = "phone is required") String phone,
        String avatar,
        @NotNull(message = "departmentId is required") Long departmentId,
        List<String> roles,
        List<String> permissions,
        Map<String, String> extFields
) {
}
