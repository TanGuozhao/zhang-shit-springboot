package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.service.DepartmentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/departments")
public class DepartmentController {

    private final DepartmentQueryService departmentQueryService;

    public DepartmentController(DepartmentQueryService departmentQueryService) {
        this.departmentQueryService = departmentQueryService;
    }

    @GetMapping("/{deptId}")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable Long deptId) {
        return ApiResponse.ok(departmentQueryService.getDepartment(deptId));
    }
}
