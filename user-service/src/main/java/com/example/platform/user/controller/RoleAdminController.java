package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.AdminRoleCreateRequest;
import com.example.platform.user.dto.AdminRolePermissionAllocateRequest;
import com.example.platform.user.dto.AdminRoleSummaryResponse;
import com.example.platform.user.dto.AdminRoleUpdateRequest;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.RolePermissionDefinitionResponse;
import com.example.platform.user.service.RoleAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/admin")
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    public RoleAdminController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @GetMapping("/roles")
    public ApiResponse<PagedResult<AdminRoleSummaryResponse>> listRoles(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(roleAdminService.listRoles(operatorUserId, sessionKey, roleName, pageNum, pageSize));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<RolePermissionDefinitionResponse>> listPermissions(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @RequestParam(value = "permissionType", required = false) String permissionType) {
        return ApiResponse.ok(roleAdminService.listPermissions(operatorUserId, sessionKey, permissionType));
    }

    @PostMapping("/roles")
    public ApiResponse<AdminRoleSummaryResponse> createRole(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody AdminRoleCreateRequest request) {
        return ApiResponse.ok(roleAdminService.createRole(operatorUserId, sessionKey, request));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<AdminRoleSummaryResponse> updateRole(
                                                            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
                                                            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
                                                            @PathVariable Long roleId,
                                                            @Valid @RequestBody AdminRoleUpdateRequest request) {
        return ApiResponse.ok(roleAdminService.updateRole(operatorUserId, sessionKey, roleId, request));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public ApiResponse<AdminRoleSummaryResponse> allocatePermissions(
                                                                     @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
                                                                     @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
                                                                     @PathVariable Long roleId,
                                                                     @Valid @RequestBody AdminRolePermissionAllocateRequest request) {
        return ApiResponse.ok(roleAdminService.allocatePermissions(operatorUserId, sessionKey, roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<Void> deleteRole(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long roleId) {
        roleAdminService.deleteRole(operatorUserId, sessionKey, roleId);
        return ApiResponse.ok("role deleted");
    }
}
