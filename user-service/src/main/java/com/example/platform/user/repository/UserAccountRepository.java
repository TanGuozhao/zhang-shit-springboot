package com.example.platform.user.repository;

import com.example.platform.user.domain.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class UserAccountRepository {

    private final ConcurrentMap<Long, UserAccount> users = new ConcurrentHashMap<>();
    private final AtomicLong userIdSequence = new AtomicLong(1002L);

    public UserAccountRepository() {
        users.put(1001L, new UserAccount(
                1001L,
                "admin",
                "admin123",
                "Platform Admin",
                "admin@example.com",
                "13800000000",
                "ENABLED",
                10L,
                List.of("ADMIN"),
                List.of("user:read", "user:write", "message:send", "log:query", "topbiz:admin")
        ));
        users.put(1002L, new UserAccount(
                1002L,
                "operator",
                "operator123",
                "Ops User",
                "operator@example.com",
                "13900000000",
                "ENABLED",
                20L,
                List.of("OPERATOR"),
                List.of("message:send", "log:query")
        ));
    }

    public Optional<UserAccount> findByAccount(String account) {
        return users.values().stream().filter(user -> user.account().equals(account)).findFirst();
    }

    public Optional<UserAccount> findByUserId(Long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public UserAccount create(String account,
                              String password,
                              String userName,
                              String email,
                              String phone,
                              Long departmentId,
                              List<String> roles,
                              List<String> permissions) {
        Long userId = userIdSequence.incrementAndGet();
        UserAccount created = new UserAccount(
                userId,
                account,
                password,
                userName,
                email,
                phone,
                "ENABLED",
                departmentId,
                List.copyOf(roles),
                List.copyOf(permissions)
        );
        users.put(userId, created);
        return created;
    }

    public Optional<UserAccount> updateStatus(Long userId, String status) {
        return Optional.ofNullable(users.computeIfPresent(userId, (key, existing) -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                existing.userName(),
                existing.email(),
                existing.phone(),
                status,
                existing.departmentId(),
                existing.roles(),
                existing.permissions()
        )));
    }

    public Optional<UserAccount> updateAuthorization(Long userId, List<String> roles, List<String> permissions) {
        return Optional.ofNullable(users.computeIfPresent(userId, (key, existing) -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                existing.userName(),
                existing.email(),
                existing.phone(),
                existing.status(),
                existing.departmentId(),
                List.copyOf(roles),
                List.copyOf(permissions)
        )));
    }
}
