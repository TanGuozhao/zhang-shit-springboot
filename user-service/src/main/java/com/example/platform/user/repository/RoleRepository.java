package com.example.platform.user.repository;

import com.example.platform.user.domain.RoleDefinition;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class RoleRepository {

    private final ConcurrentMap<Long, RoleDefinition> roles = new ConcurrentHashMap<>();
    private final AtomicLong roleIdSequence = new AtomicLong(6L);

    public RoleRepository() {
        Instant seededAt = Instant.parse("2026-01-01T00:00:00Z");
        seed(new RoleDefinition(1L, "ADMIN", "Platform Administrator", "Full platform administrator",
                List.of(
                        "user:self:read", "user:self:write", "user:read", "user:write", "user:status",
                        "role:read", "role:write", "department:read", "department:write",
                        "department:tree:read", "department:attribute:read", "department:attribute:write",
                        "department:member:read", "department:member:write",
                        "message:send", "log:query",
                        "topbiz:admin", "topbiz:platform:read", "topbiz:architecture:read",
                        "topbiz:orchestration:write", "topbiz:message:admin", "topbiz:log:admin",
                        "topbiz:runtime:operate"
                ),
                seededAt, seededAt));
        seed(new RoleDefinition(2L, "OPERATOR", "Operations User", "Daily operations role",
                List.of("user:self:read", "user:self:write", "message:send", "log:query"),
                seededAt.plusSeconds(60), seededAt.plusSeconds(60)));
        seed(new RoleDefinition(3L, "USER", "Standard User", "Default business user role",
                List.of("user:self:read", "user:self:write"),
                seededAt.plusSeconds(120), seededAt.plusSeconds(120)));
        seed(new RoleDefinition(4L, "AUDITOR", "Audit User", "Read-only audit role",
                List.of("user:read", "log:query"),
                seededAt.plusSeconds(180), seededAt.plusSeconds(180)));
        seed(new RoleDefinition(5L, "OPS_ADMIN", "Operations Administrator", "Operations administrator role",
                List.of(
                        "user:self:read", "user:self:write", "user:read",
                        "message:send", "log:query",
                        "topbiz:platform:read", "topbiz:message:admin",
                        "topbiz:log:admin", "topbiz:runtime:operate"
                ),
                seededAt.plusSeconds(240), seededAt.plusSeconds(240)));
        seed(new RoleDefinition(6L, "DEPARTMENT_ADMIN", "Department Administrator", "Scoped department administrator role",
                List.of("user:read", "user:write", "department:read", "department:write",
                        "department:tree:read", "department:attribute:read", "department:attribute:write",
                        "department:member:read", "department:member:write"),
                seededAt.plusSeconds(300), seededAt.plusSeconds(300)));
    }

    public List<RoleDefinition> findAll() {
        return roles.values().stream()
                .sorted(Comparator.comparing(RoleDefinition::roleId))
                .toList();
    }

    public Optional<RoleDefinition> findById(Long roleId) {
        return Optional.ofNullable(roles.get(roleId));
    }

    public Optional<RoleDefinition> findByCode(String roleCode) {
        return roles.values().stream()
                .filter(role -> role.roleCode().equalsIgnoreCase(roleCode))
                .findFirst();
    }

    public boolean existsAll(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return true;
        }
        return roleCodes.stream().allMatch(roleCode -> findByCode(roleCode).isPresent());
    }

    public RoleDefinition create(String roleCode,
                                 String roleName,
                                 String description,
                                 List<String> permissions) {
        Long roleId = roleIdSequence.incrementAndGet();
        Instant now = Instant.now();
        RoleDefinition created = new RoleDefinition(
                roleId,
                roleCode.trim(),
                roleName.trim(),
                description,
                List.copyOf(new ArrayList<>(permissions)),
                now,
                now
        );
        roles.put(roleId, created);
        return created;
    }

    public Optional<RoleDefinition> update(Long roleId,
                                           String roleName,
                                           String description,
                                           List<String> permissions) {
        return Optional.ofNullable(roles.computeIfPresent(roleId, (key, existing) -> new RoleDefinition(
                existing.roleId(),
                existing.roleCode(),
                roleName.trim(),
                description,
                List.copyOf(new ArrayList<>(permissions)),
                existing.createdAt(),
                Instant.now()
        )));
    }

    public Optional<RoleDefinition> updatePermissions(Long roleId, List<String> permissions) {
        return Optional.ofNullable(roles.computeIfPresent(roleId, (key, existing) -> new RoleDefinition(
                existing.roleId(),
                existing.roleCode(),
                existing.roleName(),
                existing.description(),
                List.copyOf(new ArrayList<>(permissions)),
                existing.createdAt(),
                Instant.now()
        )));
    }

    public void delete(Long roleId) {
        roles.remove(roleId);
    }

    public List<String> permissionsForRoles(List<String> roleCodes) {
        Set<String> merged = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            findByCode(roleCode).ifPresent(role -> merged.addAll(role.permissions()));
        }
        return List.copyOf(merged);
    }

    private void seed(RoleDefinition roleDefinition) {
        roles.put(roleDefinition.roleId(), roleDefinition);
        roleIdSequence.updateAndGet(current -> Math.max(current, roleDefinition.roleId()));
    }
}
