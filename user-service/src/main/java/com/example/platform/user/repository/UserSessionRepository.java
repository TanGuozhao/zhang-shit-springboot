package com.example.platform.user.repository;

import com.example.platform.user.domain.UserSession;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class UserSessionRepository {

    private final ConcurrentMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession create(Long userId) {
        String sessionKey = "SESSION-" + userId + "-" + UUID.randomUUID();
        UserSession session = new UserSession(sessionKey, userId, Instant.now());
        sessions.put(sessionKey, session);
        return session;
    }

    public Optional<UserSession> findBySessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionKey));
    }

    public void remove(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }
        sessions.remove(sessionKey);
    }
}
