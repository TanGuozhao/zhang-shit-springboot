package com.example.platform.topbiz.remote.dto;

import java.util.Map;

public record RemoteAuthLoginRequest(
        String account,
        String password,
        String loginType,
        Boolean rememberLogin,
        Map<String, String> thirdPartyInfo
) {
}
