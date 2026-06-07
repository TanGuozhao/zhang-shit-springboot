package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.Department;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DepartmentQueryService {

    private final DepartmentRepository departmentRepository;
    private final UserAccessSupport userAccessSupport;
    private final UserViewAssembler userViewAssembler;

    public DepartmentQueryService(DepartmentRepository departmentRepository,
                                  UserAccessSupport userAccessSupport,
                                  UserViewAssembler userViewAssembler) {
        this.departmentRepository = departmentRepository;
        this.userAccessSupport = userAccessSupport;
        this.userViewAssembler = userViewAssembler;
    }

    public DepartmentResponse getDepartment(Long userId, String sessionKey, Long deptId) {
        UserAccount operator = userAccessSupport.requireCurrentUser(userId, sessionKey);
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found", HttpStatus.NOT_FOUND));
        if (operator.departmentId() == null || !operator.departmentId().equals(deptId)) {
            userAccessSupport.requireScopedDepartmentAccess(userId, sessionKey, "department:read", deptId);
        }
        return userViewAssembler.toDepartmentResponse(department);
    }
}
