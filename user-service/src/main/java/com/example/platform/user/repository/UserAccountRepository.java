package com.example.platform.user.repository;

import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.support.PasswordHashCodec;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

@Repository
public class UserAccountRepository {

    private final ConcurrentMap<Long, UserAccount> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Deque<String>> passwordHistories = new ConcurrentHashMap<>();
    private final AtomicLong userIdSequence = new AtomicLong(1002L);

    public UserAccountRepository() {
        Instant seededAt = Instant.parse("2026-01-01T00:00:00Z");
        seed(new UserAccount(
                1001L,
                "admin",
                PasswordHashCodec.encode("admin123"),
                "Platform Admin",
                "admin@example.com",
                "13800000000",
                "https://cdn.example.com/avatar/admin.png",
                "ENABLED",
                10L,
                List.of("ADMIN"),
                List.of(),
                Map.of("title", "Super Administrator"),
                seededAt,
                seededAt
        ));
        seed(new UserAccount(
                1002L,
                "operator",
                PasswordHashCodec.encode("operator123"),
                "Ops User",
                "operator@example.com",
                "13900000000",
                "https://cdn.example.com/avatar/operator.png",
                "ENABLED",
                20L,
                List.of("OPERATOR"),
                List.of(),
                Map.of("shift", "day"),
                seededAt.plusSeconds(60),
                seededAt.plusSeconds(60)
        ));
    }

    public List<UserAccount> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(UserAccount::userId))
                .toList();
    }

    public Optional<UserAccount> findByAccount(String account) {
        String normalizedAccount = normalizeAccount(account);
        return users.values().stream()
                .filter(user -> user.account().equalsIgnoreCase(normalizedAccount))
                .findFirst();
    }

    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalizedEmail = email.trim();
        return users.values().stream()
                .filter(user -> user.email() != null && user.email().equalsIgnoreCase(normalizedEmail))
                .findFirst();
    }

    public Optional<UserAccount> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        String normalizedPhone = phone.trim();
        return users.values().stream()
                .filter(user -> user.phone() != null && user.phone().equals(normalizedPhone))
                .findFirst();
    }

    public Optional<UserAccount> findByAccountAndContact(String account, String contact) {
        return findByAccount(account)
                .filter(user -> matchesContact(user, contact));
    }

    public Optional<UserAccount> findByUserId(Long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public boolean existsByEmail(String email, Long excludedUserId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return users.values().stream()
                .filter(user -> excludedUserId == null || !user.userId().equals(excludedUserId))
                .anyMatch(user -> email.equalsIgnoreCase(user.email()));
    }

    public boolean existsByPhone(String phone, Long excludedUserId) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return users.values().stream()
                .filter(user -> excludedUserId == null || !user.userId().equals(excludedUserId))
                .anyMatch(user -> phone.equals(user.phone()));
    }

    public List<UserAccount> findByDepartmentId(Long departmentId) {
        return users.values().stream()
                .filter(user -> Objects.equals(user.departmentId(), departmentId))
                .sorted(Comparator.comparing(UserAccount::userId))
                .toList();
    }

    public long countByDepartmentId(Long departmentId) {
        return users.values().stream()
                .filter(user -> Objects.equals(user.departmentId(), departmentId))
                .count();
    }

    public long countByRole(String roleCode) {
        return users.values().stream()
                .filter(user -> user.roles().contains(roleCode))
                .count();
    }

    public UserAccount create(String account,
                              String password,
                              String userName,
                              String email,
                              String phone,
                              String avatar,
                              Long departmentId,
                              List<String> roles,
                              List<String> permissions,
                              Map<String, String> extFields) {
        Long userId = userIdSequence.incrementAndGet();
        Instant now = Instant.now();
        UserAccount created = new UserAccount(
                userId,
                normalizeAccount(account),
                password,
                userName,
                email,
                phone,
                avatar,
                "ENABLED",
                departmentId,
                copyList(roles),
                copyList(permissions),
                copyMap(extFields),
                now,
                now
        );
        seed(created);
        return created;
    }

    public Optional<UserAccount> updateStatus(Long userId, String status) {
        return update(userId, existing -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                existing.userName(),
                existing.email(),
                existing.phone(),
                existing.avatar(),
                status,
                existing.departmentId(),
                existing.roles(),
                existing.permissions(),
                existing.extFields(),
                existing.createdAt(),
                Instant.now()
        ));
    }

    public Optional<UserAccount> updateAuthorization(Long userId, List<String> roles, List<String> permissions) {
        return update(userId, existing -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                existing.userName(),
                existing.email(),
                existing.phone(),
                existing.avatar(),
                existing.status(),
                existing.departmentId(),
                copyList(roles),
                copyList(permissions),
                existing.extFields(),
                existing.createdAt(),
                Instant.now()
        ));
    }

    public Optional<UserAccount> updateProfile(Long userId,
                                               String userName,
                                               String email,
                                               String phone,
                                               String avatar,
                                               Map<String, String> extFields) {
        return update(userId, existing -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                userName,
                email,
                phone,
                avatar,
                existing.status(),
                existing.departmentId(),
                existing.roles(),
                existing.permissions(),
                copyMap(extFields),
                existing.createdAt(),
                Instant.now()
        ));
    }

    public Optional<UserAccount> updateUser(Long userId,
                                            String userName,
                                            String email,
                                            String phone,
                                            String avatar,
                                            Long departmentId,
                                            List<String> roles,
                                            List<String> permissions,
                                            Map<String, String> extFields) {
        return update(userId, existing -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                userName,
                email,
                phone,
                avatar,
                existing.status(),
                departmentId,
                copyList(roles),
                copyList(permissions),
                copyMap(extFields),
                existing.createdAt(),
                Instant.now()
        ));
    }

    public Optional<UserAccount> updateDepartmentMembership(Long userId, Long departmentId) {
        return update(userId, existing -> new UserAccount(
                existing.userId(),
                existing.account(),
                existing.password(),
                existing.userName(),
                existing.email(),
                existing.phone(),
                existing.avatar(),
                existing.status(),
                departmentId,
                existing.roles(),
                existing.permissions(),
                existing.extFields(),
                existing.createdAt(),
                Instant.now()
        ));
    }

    public Optional<UserAccount> updatePassword(Long userId, String newPassword) {
        Optional<UserAccount> updated = update(userId, existing -> new UserAccount(
                existing.userId(),
                existing.account(),
                newPassword,
                existing.userName(),
                existing.email(),
                existing.phone(),
                existing.avatar(),
                existing.status(),
                existing.departmentId(),
                existing.roles(),
                existing.permissions(),
                existing.extFields(),
                existing.createdAt(),
                Instant.now()
        ));
        updated.ifPresent(user -> addPasswordHistory(user.userId(), newPassword));
        return updated;
    }

    public boolean passwordUsedRecently(Long userId, String candidateRawPassword, int historyLimit) {
        Deque<String> history = passwordHistories.get(userId);
        if (history == null) {
            return false;
        }
        return history.stream()
                .limit(historyLimit)
                .anyMatch(encodedPassword -> PasswordHashCodec.matches(candidateRawPassword, encodedPassword));
    }

    private Optional<UserAccount> update(Long userId, UnaryOperator<UserAccount> updater) {
        return Optional.ofNullable(users.computeIfPresent(userId, (key, existing) -> updater.apply(existing)));
    }

    private void seed(UserAccount userAccount) {
        users.put(userAccount.userId(), userAccount);
        addPasswordHistory(userAccount.userId(), userAccount.password());
    }

    private void addPasswordHistory(Long userId, String password) {
        Deque<String> history = passwordHistories.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        history.remove(password);
        history.addFirst(password);
        while (history.size() > 10) {
            history.removeLast();
        }
    }

    private boolean matchesContact(UserAccount user, String contact) {
        if (contact == null || contact.isBlank()) {
            return false;
        }
        return contact.equalsIgnoreCase(user.email()) || contact.equals(user.phone());
    }

    private String normalizeAccount(String account) {
        return account == null ? null : account.trim();
    }

    private List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(new ArrayList<>(values));
    }

    private Map<String, String> copyMap(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(values));
    }
}
