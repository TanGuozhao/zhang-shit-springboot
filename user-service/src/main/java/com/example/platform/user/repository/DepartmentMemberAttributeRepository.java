package com.example.platform.user.repository;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class DepartmentMemberAttributeRepository {

    private final ConcurrentMap<String, Map<String, String>> memberAttributes = new ConcurrentHashMap<>();

    public Map<String, String> findByDepartmentIdAndUserId(Long departmentId, Long userId) {
        return memberAttributes.getOrDefault(key(departmentId, userId), Map.of());
    }

    public Map<String, String> mergeAttributes(Long departmentId, Long userId, Map<String, String> attributes) {
        String repositoryKey = key(departmentId, userId);
        Map<String, String> updated = memberAttributes.compute(repositoryKey, (ignored, existing) -> {
            Map<String, String> merged = new LinkedHashMap<>(existing == null ? Map.of() : existing);
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    merged.remove(entry.getKey());
                } else {
                    merged.put(entry.getKey(), entry.getValue().trim());
                }
            }
            return Map.copyOf(merged);
        });
        return updated == null ? Map.of() : updated;
    }

    public boolean existsByDepartmentIdAndAttributeKey(Long departmentId, String attributeKey) {
        return memberAttributes.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(departmentId + "::"))
                .anyMatch(entry -> entry.getValue().containsKey(attributeKey));
    }

    public void deleteByDepartmentIdAndUserId(Long departmentId, Long userId) {
        memberAttributes.remove(key(departmentId, userId));
    }

    public void deleteByDepartmentIdAndAttributeKey(Long departmentId, String attributeKey) {
        memberAttributes.replaceAll((repositoryKey, attributes) -> {
            if (!repositoryKey.startsWith(departmentId + "::") || !attributes.containsKey(attributeKey)) {
                return attributes;
            }
            Map<String, String> updated = new LinkedHashMap<>(attributes);
            updated.remove(attributeKey);
            return Map.copyOf(updated);
        });
    }

    public void deleteByDepartmentId(Long departmentId) {
        memberAttributes.keySet().removeIf(repositoryKey -> repositoryKey.startsWith(departmentId + "::"));
    }

    private String key(Long departmentId, Long userId) {
        return departmentId + "::" + userId;
    }
}
