package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemoteAuthLoginResponse(
        Long userId,
        String account,
        String userName,
        List<String> roles,
        List<String> permissions,
        String sessionKey
) {
}
