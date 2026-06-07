package com.example.platform.topbiz.remote;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.config.TopbizFeignConfig;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginRequest;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginResponse;
import com.example.platform.topbiz.remote.dto.RemoteArchitectureOverviewResponse;
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
import com.example.platform.user.dto.DepartmentMembershipResponse;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchResultResponse;
import com.example.platform.user.dto.DepartmentMemberAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberRelationRequest;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.dto.DepartmentTransferRequest;
import com.example.platform.user.dto.DepartmentUserSummaryResponse;
import com.example.platform.user.dto.ForgotPasswordResetRequest;
import com.example.platform.user.dto.ForgotPasswordSendCodeRequest;
import com.example.platform.user.dto.ForgotPasswordSendCodeResponse;
import com.example.platform.user.dto.OrganizationTreeNodeResponse;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.PasswordChangeRequest;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.RolePermissionDefinitionResponse;
import com.example.platform.user.dto.UserAuthorizationUpdateRequest;
import com.example.platform.user.dto.UserCancelRequest;
import com.example.platform.user.dto.UserCreateRequest;
import com.example.platform.user.dto.UserProfileModificationRecordResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserProfileUpdateRequest;
import com.example.platform.user.dto.UserRegistrationRequest;
import com.example.platform.user.dto.UserRegistrationResponse;
import com.example.platform.user.dto.UserStatusResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import com.example.platform.user.dto.UserUnfreezeRequest;
import com.example.platform.user.dto.VerifyCodeSendRequest;
import com.example.platform.user.dto.VerifyCodeSendResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "userServiceClient", url = "${topbiz.remote.user-service.base-url}", configuration = TopbizFeignConfig.class)
public interface UserServiceClient {

    @GetMapping("/internal/architecture/overview")
    ApiResponse<RemoteArchitectureOverviewResponse> architectureOverview();

    @PostMapping("/api/users/auth/login")
    ApiResponse<RemoteAuthLoginResponse> login(@RequestBody RemoteAuthLoginRequest request);

    @PostMapping("/api/users/auth/logout")
    ApiResponse<Void> logout(@RequestHeader("X-Session-Key") String sessionKey);

    @PostMapping("/api/users/auth/verify-codes")
    ApiResponse<VerifyCodeSendResponse> sendVerifyCode(@RequestBody VerifyCodeSendRequest request);

    @PostMapping("/api/users/auth/register")
    ApiResponse<UserRegistrationResponse> register(@RequestBody UserRegistrationRequest request);

    @GetMapping("/api/users/me")
    ApiResponse<UserProfileResponse> me();

    @PutMapping("/api/users/me")
    ApiResponse<UserProfileResponse> updateProfile(@RequestBody UserProfileUpdateRequest request);

    @GetMapping("/api/users/me/modify-records")
    ApiResponse<PagedResult<UserProfileModificationRecordResponse>> listModificationRecords(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @PutMapping("/api/users/me/password")
    ApiResponse<Void> changePassword(@RequestBody PasswordChangeRequest request);

    @PostMapping("/api/users/password/forgot/send-code")
    ApiResponse<ForgotPasswordSendCodeResponse> sendForgotPasswordCode(@RequestBody ForgotPasswordSendCodeRequest request);

    @PostMapping("/api/users/password/forgot/reset")
    ApiResponse<Void> resetForgottenPassword(@RequestBody ForgotPasswordResetRequest request);

    @GetMapping("/api/users/me/status")
    ApiResponse<UserStatusResponse> getCurrentStatus();

    @PostMapping("/api/users/me/status/unfreeze")
    ApiResponse<UserStatusResponse> applyUnfreeze(@RequestBody UserUnfreezeRequest request);

    @PostMapping("/api/users/me/status/cancel")
    ApiResponse<UserStatusResponse> applyCancel(@RequestBody UserCancelRequest request);

    @GetMapping("/api/users/{userId}")
    ApiResponse<UserProfileResponse> getUser(@PathVariable Long userId);

    @GetMapping("/api/users/{userId}/permissions")
    ApiResponse<PermissionListResponse> getPermissions(@PathVariable Long userId);

    @GetMapping("/api/users/{userId}/roles")
    ApiResponse<RoleListResponse> getRoles(@PathVariable Long userId);

    @GetMapping("/api/users/departments/{deptId}")
    ApiResponse<DepartmentResponse> getDepartment(@PathVariable Long deptId);

    @GetMapping("/api/users/admin")
    ApiResponse<PagedResult<AdminUserSummaryResponse>> listUsers(
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @PostMapping("/api/users/admin")
    ApiResponse<UserProfileResponse> createUser(@RequestBody UserCreateRequest request);

    @PutMapping("/api/users/admin/{userId}")
    ApiResponse<UserProfileResponse> updateUser(@PathVariable Long userId, @RequestBody AdminUserUpdateRequest request);

    @PatchMapping("/api/users/admin/{userId}/status")
    ApiResponse<UserProfileResponse> updateStatus(@PathVariable Long userId, @RequestBody UserStatusUpdateRequest request);

    @PatchMapping("/api/users/admin/{userId}/authorization")
    ApiResponse<RoleListResponse> updateAuthorization(@PathVariable Long userId, @RequestBody UserAuthorizationUpdateRequest request);

    @PostMapping("/api/users/admin/{userId}/authorization/permissions:refresh")
    ApiResponse<PermissionListResponse> refreshPermissions(@PathVariable Long userId);

    @PostMapping("/api/users/admin/{userId}/password:reset")
    ApiResponse<Void> resetPassword(@PathVariable Long userId, @RequestBody AdminPasswordResetRequest request);

    @GetMapping("/api/users/admin/roles")
    ApiResponse<PagedResult<AdminRoleSummaryResponse>> listRoles(
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @GetMapping("/api/users/admin/permissions")
    ApiResponse<List<RolePermissionDefinitionResponse>> listPermissionCatalog(
            @RequestParam(value = "permissionType", required = false) String permissionType);

    @PostMapping("/api/users/admin/roles")
    ApiResponse<AdminRoleSummaryResponse> createRole(@RequestBody AdminRoleCreateRequest request);

    @PutMapping("/api/users/admin/roles/{roleId}")
    ApiResponse<AdminRoleSummaryResponse> updateRole(@PathVariable Long roleId, @RequestBody AdminRoleUpdateRequest request);

    @PutMapping("/api/users/admin/roles/{roleId}/permissions")
    ApiResponse<AdminRoleSummaryResponse> allocateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody AdminRolePermissionAllocateRequest request);

    @DeleteMapping("/api/users/admin/roles/{roleId}")
    ApiResponse<Void> deleteRole(@PathVariable Long roleId);

    @GetMapping("/api/users/admin/departments")
    ApiResponse<PagedResult<AdminDepartmentSummaryResponse>> listDepartments(
            @RequestParam(value = "deptName", required = false) String deptName,
            @RequestParam(value = "parentDepartmentId", required = false) Long parentDepartmentId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @GetMapping("/api/users/admin/departments/tree")
    ApiResponse<List<OrganizationTreeNodeResponse>> getOrganizationTree();

    @PostMapping("/api/users/admin/departments")
    ApiResponse<AdminDepartmentSummaryResponse> createDepartment(@RequestBody AdminDepartmentCreateRequest request);

    @PutMapping("/api/users/admin/departments/{departmentId}")
    ApiResponse<AdminDepartmentSummaryResponse> updateDepartment(
            @PathVariable Long departmentId,
            @RequestBody AdminDepartmentUpdateRequest request);

    @DeleteMapping("/api/users/admin/departments/{departmentId}")
    ApiResponse<Void> deleteDepartment(@PathVariable Long departmentId);

    @GetMapping("/api/users/admin/departments/{departmentId}/users")
    ApiResponse<PagedResult<DepartmentUserSummaryResponse>> listDepartmentUsers(
            @PathVariable Long departmentId,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @GetMapping("/api/users/admin/departments/{departmentId}/attributes/definitions")
    ApiResponse<List<DepartmentAttributeDefinitionResponse>> listAttributeDefinitions(@PathVariable Long departmentId);

    @PostMapping("/api/users/admin/departments/{departmentId}/attributes/definitions")
    ApiResponse<DepartmentAttributeDefinitionResponse> createAttributeDefinition(
            @PathVariable Long departmentId,
            @RequestBody DepartmentAttributeDefinitionCreateRequest request);

    @DeleteMapping("/api/users/admin/departments/{departmentId}/attributes/definitions/{attributeKey}")
    ApiResponse<Void> deleteAttributeDefinition(@PathVariable Long departmentId, @PathVariable String attributeKey);

    @PutMapping("/api/users/admin/departments/{departmentId}/attributes")
    ApiResponse<AdminDepartmentSummaryResponse> updateDepartmentAttributes(
            @PathVariable Long departmentId,
            @RequestBody DepartmentAttributesUpdateRequest request);

    @PostMapping("/api/users/admin/departments/{departmentId}/members")
    ApiResponse<PagedResult<DepartmentUserSummaryResponse>> addMembers(
            @PathVariable Long departmentId,
            @RequestBody DepartmentMemberRelationRequest request);

    @DeleteMapping("/api/users/admin/departments/{departmentId}/members/{userId}")
    ApiResponse<DepartmentMembershipResponse> removeMember(@PathVariable Long departmentId, @PathVariable Long userId);

    @PostMapping("/api/users/admin/departments/transfer")
    ApiResponse<List<DepartmentMembershipResponse>> transferMembers(@RequestBody DepartmentTransferRequest request);

    @GetMapping("/api/users/admin/departments/users/{userId}/membership")
    ApiResponse<DepartmentMembershipResponse> getUserMembership(@PathVariable Long userId);

    @PutMapping("/api/users/admin/departments/{departmentId}/members/{userId}/attributes")
    ApiResponse<DepartmentMembershipResponse> updateMemberAttributes(
            @PathVariable Long departmentId,
            @PathVariable Long userId,
            @RequestBody DepartmentMemberAttributesUpdateRequest request);

    @PostMapping("/api/users/admin/departments/{departmentId}/members/attributes:batch")
    ApiResponse<DepartmentMemberAttributesBatchResultResponse> batchUpdateMemberAttributes(
            @PathVariable Long departmentId,
            @RequestBody DepartmentMemberAttributesBatchRequest request);
}
