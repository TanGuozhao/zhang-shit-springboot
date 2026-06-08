package com.example.platform.topbiz.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.config.TopbizAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class MockTopbizOAuthProviderClient implements TopbizOAuthProviderClient {

    @Override
    public boolean supports(String provider) {
        return "QQ".equalsIgnoreCase(provider) || "WECHAT".equalsIgnoreCase(provider);
    }

    @Override
    public TopbizOAuthProviderProfile exchangeCode(String provider,
                                                   String code,
                                                   TopbizAuthProperties.OAuthProvider providerConfig) {
        if (!providerConfig.isMockEnabled()) {
            throw new BusinessException(
                    "OAUTH_LIVE_EXCHANGE_NOT_IMPLEMENTED",
                    "live oauth exchange is not implemented in the current skeleton",
                    HttpStatus.NOT_IMPLEMENTED
            );
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException("OAUTH_CODE_REQUIRED", "oauth code is required");
        }

        String normalizedProvider = provider.trim().toUpperCase(Locale.ROOT);
        String normalizedCode = code.trim();
        String subject = normalizedProvider.toLowerCase(Locale.ROOT) + "_" + normalizedCode.replaceAll("[^A-Za-z0-9_\\-]", "_");
        String email = subject + "@example.com";
        String avatar = providerConfig.getDefaultAvatarUrl() == null || providerConfig.getDefaultAvatarUrl().isBlank()
                ? "https://cdn.example.com/avatar/" + normalizedProvider.toLowerCase(Locale.ROOT) + ".png"
                : providerConfig.getDefaultAvatarUrl();

        Map<String, String> rawProfile = new LinkedHashMap<>();
        rawProfile.put("provider", normalizedProvider);
        rawProfile.put("mock", "true");
        rawProfile.put("code", normalizedCode);
        rawProfile.put("email", email);

        return new TopbizOAuthProviderProfile(
                normalizedProvider,
                subject,
                normalizedProvider.toLowerCase(Locale.ROOT) + "_union_" + normalizedCode,
                email,
                email,
                null,
                normalizedProvider + " User",
                avatar,
                Map.copyOf(rawProfile)
        );
    }
}
