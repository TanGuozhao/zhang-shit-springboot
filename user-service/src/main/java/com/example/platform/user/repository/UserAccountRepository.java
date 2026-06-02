package com.example.platform.user.repository;

import com.example.platform.user.domain.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserAccountRepository {

    private final List<UserAccount> users = List.of(
            new UserAccount(
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
            ),
            new UserAccount(
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
            )
    );

    public Optional<UserAccount> findByAccount(String account) {
        return users.stream().filter(user -> user.account().equals(account)).findFirst();
    }

    public Optional<UserAccount> findByUserId(Long userId) {
        return users.stream().filter(user -> user.userId().equals(userId)).findFirst();
    }
}
