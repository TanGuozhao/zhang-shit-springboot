package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.security.TopbizPermissions;
import com.example.platform.topbiz.service.TopbizUserGatewayService;
import com.example.platform.user.dto.AdminDepartmentCreateRequest;
import com.example.platform.user.dto.AdminDepartmentSummaryResponse;
import com.example.platform.user.dto.AdminDepartmentUpdateRequest;
import com.example.platform.user.dto.AdminPasswordResetRequest;
import com.example.platform.user.dto.AdminRoleCreateRequest;
import com.example.platform.user.dto.AdminRolePermissionAllocateRequest;
import com.example.platform.user.dto.AdminRoleSummaryResponse;
import com.example.platform.user.dto.AdminRoleUpdateRequest;
import com.example.platform.user.dto.AdminUserSummaryResponse;
import com.example.platform.user.dto.AdminUserUpdateRequest;
import com.example.platform.user.dto.DepartmentAttributeDefinitionCreateRequest;
import com.example.platform.user.dto.DepartmentAttributeDefinitionResponse;
import com.example.platform.user.dto.DepartmentAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchResultResponse;
import com.example.platform.user.dto.DepartmentMemberAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberRelationRequest;
import com.example.platform.user.dto.DepartmentMembershipResponse;
import com.example.platform.user.dto.DepartmentTransferRequest;
import com.example.platform.user.dto.DepartmentUserSummaryResponse;
import com.example.platform.user.dto.OrganizationTreeNodeResponse;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.RolePermissionDefinitionResponse;
import com.example.platform.user.dto.UserAuthorizationUpdateRequest;
import com.example.platform.user.dto.UserCreateRequest;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topbiz/users/admin")
public class TopbizUserAdminController {

    private final TopbizUserGatewayService topbizUserGatewayService;

    public TopbizUserAdminController(TopbizUserGatewayService topbizUserGatewayService) {
        this.topbizUserGatewayService = topbizUserGatewayService;
    }

    @GetMapping
    @RequiresPermissions(TopbizPermissions.USER_READ)
    public ApiResponse<PagedResult<AdminUserSummaryResponse>> listUsers(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(topbizUserGatewayService.listUsers(account, userName, status, pageNum, pageSize));
    }

    @PostMapping
    @RequiresPermissions(TopbizPermissions.USER_WRITE)
    public ApiResponse<UserProfileResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.createUser(request));
    }

    @PutMapping("/{userId}")
    @RequiresPermissions(TopbizPermissions.USER_WRITE)
    public ApiResponse<UserProfileResponse> updateUser(@PathVariable Long userId,
                                                       @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateUser(userId, request));
    }

    @PatchMapping("/{userId}/status")
    @RequiresPermissions(TopbizPermissions.USER_STATUS)
    public ApiResponse<UserProfileResponse> updateStatus(@PathVariable Long userId,
                                                         @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateStatus(userId, request));
    }

    @PatchMapping("/{userId}/authorization")
    @RequiresPermissions(TopbizPermissions.USER_WRITE)
    public ApiResponse<RoleListResponse> updateAuthorization(@PathVariable Long userId,
                                                             @Valid @RequestBody UserAuthorizationUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateAuthorization(userId, request));
    }

    @PostMapping("/{userId}/authorization/permissions:refresh")
    @RequiresPermissions(TopbizPermissions.USER_READ)
    public ApiResponse<PermissionListResponse> refreshPermissions(@PathVariable Long userId) {
        return ApiResponse.ok(topbizUserGatewayService.refreshPermissions(userId));
    }

    @PostMapping("/{userId}/password:reset")
    @RequiresPermissions(TopbizPermissions.USER_WRITE)
    public ApiResponse<Void> resetPassword(@PathVariable Long userId,
                                           @Valid @RequestBody AdminPasswordResetRequest request) {
        topbizUserGatewayService.resetPassword(userId, request);
        return ApiResponse.ok("password reset");
    }

    @GetMapping("/roles")
    @RequiresPermissions(TopbizPermissions.ROLE_READ)
    public ApiResponse<PagedResult<AdminRoleSummaryResponse>> listRoles(
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(topbizUserGatewayService.listRoles(roleName, pageNum, pageSize));
    }

    @GetMapping("/permissions")
    @RequiresPermissions(TopbizPermissions.ROLE_READ)
    public ApiResponse<List<RolePermissionDefinitionResponse>> listPermissionCatalog(
            @RequestParam(value = "permissionType", required = false) String permissionType) {
        return ApiResponse.ok(topbizUserGatewayService.listPermissionCatalog(permissionType));
    }

    @PostMapping("/roles")
    @RequiresPermissions(TopbizPermissions.ROLE_WRITE)
    public ApiResponse<AdminRoleSummaryResponse> createRole(@Valid @RequestBody AdminRoleCreateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    @RequiresPermissions(TopbizPermissions.ROLE_WRITE)
    public ApiResponse<AdminRoleSummaryResponse> updateRole(@PathVariable Long roleId,
                                                            @Valid @RequestBody AdminRoleUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateRole(roleId, request));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequiresPermissions(TopbizPermissions.ROLE_WRITE)
    public ApiResponse<AdminRoleSummaryResponse> allocateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody AdminRolePermissionAllocateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.allocateRolePermissions(roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    @RequiresPermissions(TopbizPermissions.ROLE_WRITE)
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId) {
        topbizUserGatewayService.deleteRole(roleId);
        return ApiResponse.ok("role deleted");
    }

    @GetMapping("/departments")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_READ)
    public ApiResponse<PagedResult<AdminDepartmentSummaryResponse>> listDepartments(
            @RequestParam(value = "deptName", required = false) String deptName,
            @RequestParam(value = "parentDepartmentId", required = false) Long parentDepartmentId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(topbizUserGatewayService.listDepartments(
                deptName, parentDepartmentId, pageNum, pageSize
        ));
    }

    @GetMapping("/departments/tree")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_TREE_READ)
    public ApiResponse<List<OrganizationTreeNodeResponse>> getOrganizationTree() {
        return ApiResponse.ok(topbizUserGatewayService.getOrganizationTree());
    }

    @PostMapping("/departments")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_WRITE)
    public ApiResponse<AdminDepartmentSummaryResponse> createDepartment(
            @Valid @RequestBody AdminDepartmentCreateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.createDepartment(request));
    }

    @PutMapping("/departments/{departmentId}")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_WRITE)
    public ApiResponse<AdminDepartmentSummaryResponse> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody AdminDepartmentUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateDepartment(departmentId, request));
    }

    @DeleteMapping("/departments/{departmentId}")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_WRITE)
    public ApiResponse<Void> deleteDepartment(@PathVariable Long departmentId) {
        topbizUserGatewayService.deleteDepartment(departmentId);
        return ApiResponse.ok("department deleted");
    }

    @GetMapping("/departments/{departmentId}/users")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_READ)
    public ApiResponse<PagedResult<DepartmentUserSummaryResponse>> listDepartmentUsers(
            @PathVariable Long departmentId,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(topbizUserGatewayService.listDepartmentUsers(
                departmentId, account, userName, pageNum, pageSize
        ));
    }

    @GetMapping("/departments/{departmentId}/attributes/definitions")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_ATTRIBUTE_READ)
    public ApiResponse<List<DepartmentAttributeDefinitionResponse>> listAttributeDefinitions(@PathVariable Long departmentId) {
        return ApiResponse.ok(topbizUserGatewayService.listAttributeDefinitions(departmentId));
    }

    @PostMapping("/departments/{departmentId}/attributes/definitions")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_ATTRIBUTE_WRITE)
    public ApiResponse<DepartmentAttributeDefinitionResponse> createAttributeDefinition(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentAttributeDefinitionCreateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.createAttributeDefinition(departmentId, request));
    }

    @DeleteMapping("/departments/{departmentId}/attributes/definitions/{attributeKey}")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_ATTRIBUTE_WRITE)
    public ApiResponse<Void> deleteAttributeDefinition(@PathVariable Long departmentId,
                                                       @PathVariable String attributeKey) {
        topbizUserGatewayService.deleteAttributeDefinition(departmentId, attributeKey);
        return ApiResponse.ok("attribute definition deleted");
    }

    @PutMapping("/departments/{departmentId}/attributes")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_ATTRIBUTE_WRITE)
    public ApiResponse<AdminDepartmentSummaryResponse> updateDepartmentAttributes(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentAttributesUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateDepartmentAttributes(departmentId, request));
    }

    @PostMapping("/departments/{departmentId}/members")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_WRITE)
    public ApiResponse<PagedResult<DepartmentUserSummaryResponse>> addMembers(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentMemberRelationRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.addMembers(departmentId, request));
    }

    @DeleteMapping("/departments/{departmentId}/members/{userId}")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_WRITE)
    public ApiResponse<DepartmentMembershipResponse> removeMember(@PathVariable Long departmentId,
                                                                  @PathVariable Long userId) {
        return ApiResponse.ok(topbizUserGatewayService.removeMember(departmentId, userId));
    }

    @PostMapping("/departments/transfer")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_WRITE)
    public ApiResponse<List<DepartmentMembershipResponse>> transferMembers(
            @Valid @RequestBody DepartmentTransferRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.transferMembers(request));
    }

    @GetMapping("/departments/users/{userId}/membership")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_READ)
    public ApiResponse<DepartmentMembershipResponse> getUserMembership(@PathVariable Long userId) {
        return ApiResponse.ok(topbizUserGatewayService.getUserMembership(userId));
    }

    @PutMapping("/departments/{departmentId}/members/{userId}/attributes")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_WRITE)
    public ApiResponse<DepartmentMembershipResponse> updateMemberAttributes(
            @PathVariable Long departmentId,
            @PathVariable Long userId,
            @Valid @RequestBody DepartmentMemberAttributesUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateMemberAttributes(departmentId, userId, request));
    }

    @PostMapping("/departments/{departmentId}/members/attributes:batch")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_MEMBER_WRITE)
    public ApiResponse<DepartmentMemberAttributesBatchResultResponse> batchUpdateMemberAttributes(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentMemberAttributesBatchRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.batchUpdateMemberAttributes(departmentId, request));
    }
}
