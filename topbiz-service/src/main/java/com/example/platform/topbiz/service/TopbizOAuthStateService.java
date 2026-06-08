package com.example.platform.topbiz.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.config.TopbizAuthProperties;
import com.example.platform.topbiz.domain.TopbizOAuthState;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TopbizOAuthStateService {

    private static final String SESSION_ATTRIBUTE = "TOPBIZ_OAUTH_STATES";

    private final TopbizAuthProperties topbizAuthProperties;

    public TopbizOAuthStateService(TopbizAuthProperties topbizAuthProperties) {
        this.topbizAuthProperties = topbizAuthProperties;
    }

    public TopbizOAuthState issue(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        Instant now = Instant.now();
        TopbizOAuthState state = new TopbizOAuthState(
                normalizedProvider,
                UUID.randomUUID().toString().replace("-", ""),
                now,
                now.plus(topbizAuthProperties.getOauthStateTtl())
        );
        stateMap(true).put(normalizedProvider, state);
        return state;
    }

    public void verifyAndConsume(String provider, String state) {
        String normalizedProvider = normalizeProvider(provider);
        TopbizOAuthState saved = stateMap(false).remove(normalizedProvider);
        if (saved == null) {
            throw new BusinessException("OAUTH_STATE_MISSING", "oauth state is missing", HttpStatus.UNAUTHORIZED);
        }
        if (!saved.state().equals(state)) {
            throw new BusinessException("OAUTH_STATE_INVALID", "oauth state is invalid", HttpStatus.UNAUTHORIZED);
        }
        if (saved.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException("OAUTH_STATE_EXPIRED", "oauth state has expired", HttpStatus.UNAUTHORIZED);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, TopbizOAuthState> stateMap(boolean create) {
        HttpSession session = currentSession(create);
        if (session == null) {
            return new LinkedHashMap<>();
        }
        Object attribute = session.getAttribute(SESSION_ATTRIBUTE);
        if (attribute instanceof Map<?, ?> map) {
            return (Map<String, TopbizOAuthState>) map;
        }
        Map<String, TopbizOAuthState> states = new LinkedHashMap<>();
        session.setAttribute(SESSION_ATTRIBUTE, states);
        return states;
    }

    private HttpSession currentSession(boolean create) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException("REQUEST_CONTEXT_MISSING", "request context is not available", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return attributes.getRequest().getSession(create);
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException("PROVIDER_REQUIRED", "provider is required");
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }
}
