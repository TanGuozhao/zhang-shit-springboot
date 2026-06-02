package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.Department;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

@Service
public class DepartmentQueryService {

    private final DepartmentRepository departmentRepository;

    public DepartmentQueryService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public DepartmentResponse getDepartment(Long deptId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found"));
        return new DepartmentResponse(
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                department.parentDepartmentId()
        );
    }
}
