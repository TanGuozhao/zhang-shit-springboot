package com.example.platform.topbiz.dto;

public record OAuthAuthorizeResponse(
        String provider,
        String authorizationUrl,
        String state,
        String redirectUri
) {
}
