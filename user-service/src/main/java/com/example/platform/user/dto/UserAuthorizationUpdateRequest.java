package com.example.platform.user.dto;

import java.util.List;

public record UserAuthorizationUpdateRequest(
        List<String> roles,
        List<String> permissions
) {
}
