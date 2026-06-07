package com.example.platform.user.repository;

import com.example.platform.user.domain.DepartmentAttributeDefinition;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class DepartmentAttributeDefinitionRepository {

    private final ConcurrentMap<String, DepartmentAttributeDefinition> definitions = new ConcurrentHashMap<>();
    private final AtomicLong attributeIdSequence = new AtomicLong(10L);

    public DepartmentAttributeDefinitionRepository() {
        Instant seededAt = Instant.parse("2026-01-01T00:00:00Z");
        seed(new DepartmentAttributeDefinition(
                1L,
                10L,
                "officeLocation",
                "Office Location",
                "TEXT",
                "Building A",
                false,
                null,
                1,
                seededAt,
                seededAt
        ));
        seed(new DepartmentAttributeDefinition(
                2L,
                20L,
                "shiftModel",
                "Shift Model",
                "TEXT",
                "24x7",
                false,
                null,
                1,
                seededAt.plusSeconds(60),
                seededAt.plusSeconds(60)
        ));
    }

    public List<DepartmentAttributeDefinition> findByDepartmentId(Long departmentId) {
        return definitions.values().stream()
                .filter(definition -> definition.departmentId().equals(departmentId))
                .sorted(Comparator.comparing(DepartmentAttributeDefinition::displayOrder)
                        .thenComparing(DepartmentAttributeDefinition::attributeId))
                .toList();
    }

    public Optional<DepartmentAttributeDefinition> findByDepartmentIdAndKey(Long departmentId, String attributeKey) {
        return Optional.ofNullable(definitions.get(key(departmentId, attributeKey)));
    }

    public boolean exists(Long departmentId, String attributeKey) {
        return definitions.containsKey(key(departmentId, attributeKey));
    }

    public boolean existsAll(Long departmentId, List<String> attributeKeys) {
        if (attributeKeys == null || attributeKeys.isEmpty()) {
            return true;
        }
        return attributeKeys.stream().allMatch(attributeKey -> exists(departmentId, attributeKey));
    }

    public DepartmentAttributeDefinition create(Long departmentId,
                                                String attributeKey,
                                                String attributeName,
                                                String dataType,
                                                String defaultValue,
                                                boolean required,
                                                String rules,
                                                Integer displayOrder) {
        Long attributeId = attributeIdSequence.incrementAndGet();
        Instant now = Instant.now();
        DepartmentAttributeDefinition created = new DepartmentAttributeDefinition(
                attributeId,
                departmentId,
                attributeKey.trim(),
                attributeName.trim(),
                dataType.trim().toUpperCase(Locale.ROOT),
                normalizeNullable(defaultValue),
                required,
                normalizeNullable(rules),
                displayOrder == null ? 999 : displayOrder,
                now,
                now
        );
        definitions.put(key(departmentId, attributeKey), created);
        return created;
    }

    public DepartmentAttributeDefinition createInferred(Long departmentId, String attributeKey, String defaultValue) {
        return create(
                departmentId,
                attributeKey,
                attributeKey,
                "TEXT",
                defaultValue,
                false,
                null,
                999
        );
    }

    public void delete(Long departmentId, String attributeKey) {
        definitions.remove(key(departmentId, attributeKey));
    }

    public void deleteByDepartmentId(Long departmentId) {
        definitions.keySet().removeIf(repositoryKey -> repositoryKey.startsWith(departmentId + "::"));
    }

    private void seed(DepartmentAttributeDefinition definition) {
        definitions.put(key(definition.departmentId(), definition.attributeKey()), definition);
        attributeIdSequence.updateAndGet(current -> Math.max(current, definition.attributeId()));
    }

    private String key(Long departmentId, String attributeKey) {
        return departmentId + "::" + attributeKey.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
