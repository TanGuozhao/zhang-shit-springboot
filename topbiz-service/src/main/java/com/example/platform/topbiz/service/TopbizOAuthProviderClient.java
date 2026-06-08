package com.example.platform.topbiz.service;

import com.example.platform.topbiz.config.TopbizAuthProperties;

public interface TopbizOAuthProviderClient {

    boolean supports(String provider);

    TopbizOAuthProviderProfile exchangeCode(String provider,
                                            String code,
                                            TopbizAuthProperties.OAuthProvider providerConfig);
}
