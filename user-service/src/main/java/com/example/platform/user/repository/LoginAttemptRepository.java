package com.example.platform.user.repository;

import com.example.platform.user.domain.LoginAttemptState;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class LoginAttemptRepository {

    private final ConcurrentMap<String, LoginAttemptState> attempts = new ConcurrentHashMap<>();

    public int recordFailure(String account) {
        String normalizedAccount = normalizeAccount(account);
        LoginAttemptState state = attempts.compute(normalizedAccount, (ignored, existing) -> {
            int nextCount = existing == null ? 1 : existing.failedCount() + 1;
            return new LoginAttemptState(normalizedAccount, nextCount, Instant.now());
        });
        return state.failedCount();
    }

    public Optional<LoginAttemptState> findByAccount(String account) {
        return Optional.ofNullable(attempts.get(normalizeAccount(account)));
    }

    public void clear(String account) {
        attempts.remove(normalizeAccount(account));
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim().toLowerCase();
    }
}
