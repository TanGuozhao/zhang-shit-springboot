package com.example.platform.user.repository;

import com.example.platform.user.domain.Department;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DepartmentRepository {

    private final List<Department> departments = List.of(
            new Department(10L, "PLATFORM", "Platform Department", 0L),
            new Department(20L, "OPS", "Operations Department", 10L)
    );

    public Optional<Department> findById(Long departmentId) {
        return departments.stream()
                .filter(department -> department.departmentId().equals(departmentId))
                .findFirst();
    }
}
