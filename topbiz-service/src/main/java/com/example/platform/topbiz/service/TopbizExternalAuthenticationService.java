package com.example.platform.topbiz.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.config.TopbizAuthProperties;
import com.example.platform.topbiz.dto.LoginResponse;
import com.example.platform.topbiz.dto.OAuthAuthorizeResponse;
import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginResponse;
import com.example.platform.user.dto.EmailCodeLoginRequest;
import com.example.platform.user.dto.EmailLoginSendCodeRequest;
import com.example.platform.user.dto.EmailLoginSendCodeResponse;
import com.example.platform.user.dto.ThirdPartyLoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;

@Service
public class TopbizExternalAuthenticationService {

    private final UserServiceClient userServiceClient;
    private final RemoteCallSupport remoteCallSupport;
    private final TopbizAuthenticationService topbizAuthenticationService;
    private final TopbizAuthProperties topbizAuthProperties;
    private final TopbizOAuthStateService topbizOAuthStateService;
    private final List<TopbizOAuthProviderClient> providerClients;

    public TopbizExternalAuthenticationService(UserServiceClient userServiceClient,
                                               RemoteCallSupport remoteCallSupport,
                                               TopbizAuthenticationService topbizAuthenticationService,
                                               TopbizAuthProperties topbizAuthProperties,
                                               TopbizOAuthStateService topbizOAuthStateService,
                                               List<TopbizOAuthProviderClient> providerClients) {
        this.userServiceClient = userServiceClient;
        this.remoteCallSupport = remoteCallSupport;
        this.topbizAuthenticationService = topbizAuthenticationService;
        this.topbizAuthProperties = topbizAuthProperties;
        this.topbizOAuthStateService = topbizOAuthStateService;
        this.providerClients = providerClients;
    }

    public EmailLoginSendCodeResponse sendEmailLoginCode(EmailLoginSendCodeRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.sendEmailLoginCode(request));
    }

    public LoginResponse loginByEmailCode(EmailCodeLoginRequest request) {
        RemoteAuthLoginResponse remote = remoteCallSupport.unwrap(userServiceClient.loginByEmailCode(request));
        return topbizAuthenticationService.establishPlatformSession(remote);
    }

    public OAuthAuthorizeResponse buildAuthorizeResponse(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        TopbizAuthProperties.OAuthProvider providerConfig = requireProvider(normalizedProvider);
        if (!providerConfig.isEnabled()) {
            throw new BusinessException("OAUTH_PROVIDER_DISABLED", "oauth provider is disabled", HttpStatus.NOT_FOUND);
        }

        var issued = topbizOAuthStateService.issue(normalizedProvider);
        String redirectUri = callbackUri(normalizedProvider);
        String authorizationUrl = buildAuthorizationUrl(normalizedProvider, providerConfig, issued.state(), redirectUri);
        return new OAuthAuthorizeResponse(normalizedProvider, authorizationUrl, issued.state(), redirectUri);
    }

    public LoginResponse completeOAuthLogin(String provider, String code, String state) {
        String normalizedProvider = normalizeProvider(provider);
        topbizOAuthStateService.verifyAndConsume(normalizedProvider, state);
        TopbizAuthProperties.OAuthProvider providerConfig = requireProvider(normalizedProvider);
        TopbizOAuthProviderProfile profile = providerClients.stream()
                .filter(client -> client.supports(normalizedProvider))
                .findFirst()
                .orElseThrow(() -> new BusinessException("OAUTH_PROVIDER_UNSUPPORTED", "oauth provider is unsupported"))
                .exchangeCode(normalizedProvider, code, providerConfig);

        ThirdPartyLoginRequest request = new ThirdPartyLoginRequest(
                profile.provider(),
                profile.providerUserId(),
                profile.providerUnionId(),
                profile.account(),
                profile.email(),
                profile.phone(),
                profile.userName(),
                profile.avatar(),
                profile.rawProfile(),
                Boolean.TRUE
        );
        RemoteAuthLoginResponse remote = remoteCallSupport.unwrap(userServiceClient.loginByThirdParty(request));
        return topbizAuthenticationService.establishPlatformSession(remote);
    }

    private String buildAuthorizationUrl(String provider,
                                         TopbizAuthProperties.OAuthProvider providerConfig,
                                         String state,
                                         String redirectUri) {
        if (providerConfig.getAuthorizeUrl() == null || providerConfig.getAuthorizeUrl().isBlank()) {
            throw new BusinessException("OAUTH_AUTHORIZE_URL_MISSING", "oauth authorize url is not configured");
        }
        if ("QQ".equals(provider)) {
            return UriComponentsBuilder.fromUriString(providerConfig.getAuthorizeUrl())
                    .queryParam("response_type", "code")
                    .queryParam("client_id", providerConfig.getClientId())
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("state", state)
                    .build()
                    .encode()
                    .toUriString();
        }
        return UriComponentsBuilder.fromUriString(providerConfig.getAuthorizeUrl())
                .queryParam("appid", providerConfig.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", providerConfig.getScope())
                .queryParam("state", state)
                .fragment("wechat_redirect")
                .build()
                .encode()
                .toUriString();
    }

    private String callbackUri(String provider) {
        return topbizAuthProperties.getPublicBaseUrl() + "/api/topbiz/auth/oauth/" + provider.toLowerCase(Locale.ROOT) + "/callback";
    }

    private TopbizAuthProperties.OAuthProvider requireProvider(String provider) {
        TopbizAuthProperties.OAuthProvider providerConfig = topbizAuthProperties.provider(provider);
        if (providerConfig == null) {
            throw new BusinessException("OAUTH_PROVIDER_UNSUPPORTED", "oauth provider is unsupported", HttpStatus.NOT_FOUND);
        }
        return providerConfig;
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException("PROVIDER_REQUIRED", "provider is required");
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }
}
