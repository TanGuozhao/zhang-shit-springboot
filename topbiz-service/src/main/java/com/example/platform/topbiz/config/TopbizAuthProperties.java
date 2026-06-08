package com.example.platform.topbiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "topbiz.auth")
public class TopbizAuthProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private Duration oauthStateTtl = Duration.ofMinutes(10);
    private final OAuthProvider qq = new OAuthProvider();
    private final OAuthProvider wechat = new OAuthProvider();

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Duration getOauthStateTtl() {
        return oauthStateTtl;
    }

    public void setOauthStateTtl(Duration oauthStateTtl) {
        this.oauthStateTtl = oauthStateTtl;
    }

    public OAuthProvider getQq() {
        return qq;
    }

    public OAuthProvider getWechat() {
        return wechat;
    }

    public OAuthProvider provider(String provider) {
        if (provider == null) {
            return null;
        }
        return switch (provider.trim().toUpperCase()) {
            case "QQ" -> qq;
            case "WECHAT" -> wechat;
            default -> null;
        };
    }

    public static class OAuthProvider {

        private boolean enabled = true;
        private boolean mockEnabled = true;
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;
        private String scope;
        private String defaultAvatarUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMockEnabled() {
            return mockEnabled;
        }

        public void setMockEnabled(boolean mockEnabled) {
            this.mockEnabled = mockEnabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public void setAuthorizeUrl(String authorizeUrl) {
            this.authorizeUrl = authorizeUrl;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getDefaultAvatarUrl() {
            return defaultAvatarUrl;
        }

        public void setDefaultAvatarUrl(String defaultAvatarUrl) {
            this.defaultAvatarUrl = defaultAvatarUrl;
        }
    }
}
