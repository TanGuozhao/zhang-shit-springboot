package com.example.platform.user.repository;

import com.example.platform.user.config.UserServiceProperties;
import com.example.platform.user.domain.UserSession;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class UserSessionRepository {

    private final ConcurrentMap<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final Duration sessionTtl;

    public UserSessionRepository(UserServiceProperties userServiceProperties) {
        this.sessionTtl = Duration.ofHours(userServiceProperties.sessionTtlHours());
    }

    public UserSession create(Long userId) {
        String sessionKey = "SESSION-" + userId + "-" + UUID.randomUUID();
        Instant issuedAt = Instant.now();
        UserSession session = new UserSession(sessionKey, userId, issuedAt, issuedAt.plus(sessionTtl));
        sessions.put(sessionKey, session);
        return session;
    }

    public Optional<UserSession> findBySessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return Optional.empty();
        }
        UserSession session = sessions.get(sessionKey);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(sessionKey);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void remove(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }
        sessions.remove(sessionKey);
    }

    public void removeByUserId(Long userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }
}
