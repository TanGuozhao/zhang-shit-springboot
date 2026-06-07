package com.example.platform.user.repository;

import com.example.platform.user.domain.Department;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class DepartmentRepository {

    private final ConcurrentMap<Long, Department> departments = new ConcurrentHashMap<>();
    private final AtomicLong departmentIdSequence = new AtomicLong(20L);

    public DepartmentRepository() {
        Instant seededAt = Instant.parse("2026-01-01T00:00:00Z");
        departments.put(10L, new Department(
                10L,
                "PLATFORM",
                "Platform Department",
                0L,
                "Platform root department",
                Map.of("officeLocation", "Building A"),
                seededAt,
                seededAt
        ));
        departments.put(20L, new Department(
                20L,
                "OPS",
                "Operations Department",
                10L,
                "Operations delivery team",
                Map.of("shiftModel", "24x7"),
                seededAt.plusSeconds(60),
                seededAt.plusSeconds(60)
        ));
    }

    public List<Department> findAll() {
        return departments.values().stream()
                .sorted(Comparator.comparing(Department::departmentId))
                .toList();
    }

    public Optional<Department> findById(Long departmentId) {
        return Optional.ofNullable(departments.get(departmentId));
    }

    public boolean existsByCode(String departmentCode) {
        return departments.values().stream()
                .anyMatch(department -> department.departmentCode().equalsIgnoreCase(departmentCode));
    }

    public boolean hasChildren(Long departmentId) {
        return departments.values().stream()
                .anyMatch(department -> department.parentDepartmentId().equals(departmentId));
    }

    public List<Long> subtreeIds(Long rootDepartmentId) {
        if (rootDepartmentId == null) {
            return List.of();
        }
        Set<Long> visited = new LinkedHashSet<>();
        collectSubtreeIds(rootDepartmentId, visited);
        return List.copyOf(visited);
    }

    public boolean isDescendantOrSelf(Long rootDepartmentId, Long targetDepartmentId) {
        if (rootDepartmentId == null || targetDepartmentId == null) {
            return false;
        }
        return subtreeIds(rootDepartmentId).contains(targetDepartmentId);
    }

    public Department create(String departmentCode,
                             String departmentName,
                             Long parentDepartmentId,
                             String description,
                             Map<String, String> attributes) {
        Long departmentId = departmentIdSequence.incrementAndGet();
        Instant now = Instant.now();
        Department created = new Department(
                departmentId,
                departmentCode.trim(),
                departmentName.trim(),
                parentDepartmentId,
                description,
                copyAttributes(attributes),
                now,
                now
        );
        departments.put(departmentId, created);
        return created;
    }

    public Optional<Department> update(Long departmentId,
                                       String departmentName,
                                       Long parentDepartmentId,
                                       String description,
                                       Map<String, String> attributes) {
        return Optional.ofNullable(departments.computeIfPresent(departmentId, (key, existing) -> new Department(
                existing.departmentId(),
                existing.departmentCode(),
                departmentName.trim(),
                parentDepartmentId,
                description,
                copyAttributes(attributes),
                existing.createdAt(),
                Instant.now()
        )));
    }

    public Optional<Department> updateAttributes(Long departmentId, Map<String, String> attributes) {
        return Optional.ofNullable(departments.computeIfPresent(departmentId, (key, existing) -> new Department(
                existing.departmentId(),
                existing.departmentCode(),
                existing.departmentName(),
                existing.parentDepartmentId(),
                existing.description(),
                copyAttributes(attributes),
                existing.createdAt(),
                Instant.now()
        )));
    }

    public void delete(Long departmentId) {
        departments.remove(departmentId);
    }

    private Map<String, String> copyAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(attributes));
    }

    private void collectSubtreeIds(Long departmentId, Set<Long> visited) {
        if (!visited.add(departmentId)) {
            return;
        }
        departments.values().stream()
                .filter(department -> department.parentDepartmentId().equals(departmentId))
                .map(Department::departmentId)
                .sorted()
                .forEach(childId -> collectSubtreeIds(childId, visited));
    }
}
