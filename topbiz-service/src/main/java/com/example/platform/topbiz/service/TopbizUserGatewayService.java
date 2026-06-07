package com.example.platform.topbiz.service;

import com.example.platform.topbiz.remote.UserServiceClient;
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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopbizUserGatewayService {

    private final UserServiceClient userServiceClient;
    private final RemoteCallSupport remoteCallSupport;

    public TopbizUserGatewayService(UserServiceClient userServiceClient,
                                    RemoteCallSupport remoteCallSupport) {
        this.userServiceClient = userServiceClient;
        this.remoteCallSupport = remoteCallSupport;
    }

    public VerifyCodeSendResponse sendVerifyCode(VerifyCodeSendRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.sendVerifyCode(request));
    }

    public UserRegistrationResponse register(UserRegistrationRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.register(request));
    }

    public UserProfileResponse currentUser() {
        return remoteCallSupport.unwrap(userServiceClient.me());
    }

    public UserProfileResponse updateProfile(UserProfileUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateProfile(request));
    }

    public PagedResult<UserProfileModificationRecordResponse> listModificationRecords(Integer pageNum, Integer pageSize) {
        return remoteCallSupport.unwrap(userServiceClient.listModificationRecords(pageNum, pageSize));
    }

    public void changePassword(PasswordChangeRequest request) {
        remoteCallSupport.ensureOk(userServiceClient.changePassword(request));
    }

    public ForgotPasswordSendCodeResponse sendForgotPasswordCode(ForgotPasswordSendCodeRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.sendForgotPasswordCode(request));
    }

    public void resetForgottenPassword(ForgotPasswordResetRequest request) {
        remoteCallSupport.ensureOk(userServiceClient.resetForgottenPassword(request));
    }

    public UserStatusResponse currentStatus() {
        return remoteCallSupport.unwrap(userServiceClient.getCurrentStatus());
    }

    public UserStatusResponse applyUnfreeze(UserUnfreezeRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.applyUnfreeze(request));
    }

    public UserStatusResponse applyCancel(UserCancelRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.applyCancel(request));
    }

    public UserProfileResponse getUser(Long userId) {
        return remoteCallSupport.unwrap(userServiceClient.getUser(userId));
    }

    public PermissionListResponse getPermissions(Long userId) {
        return remoteCallSupport.unwrap(userServiceClient.getPermissions(userId));
    }

    public RoleListResponse getRoles(Long userId) {
        return remoteCallSupport.unwrap(userServiceClient.getRoles(userId));
    }

    public DepartmentResponse getDepartment(Long departmentId) {
        return remoteCallSupport.unwrap(userServiceClient.getDepartment(departmentId));
    }

    public PagedResult<AdminUserSummaryResponse> listUsers(String account,
                                                           String userName,
                                                           String status,
                                                           Integer pageNum,
                                                           Integer pageSize) {
        return remoteCallSupport.unwrap(userServiceClient.listUsers(account, userName, status, pageNum, pageSize));
    }

    public UserProfileResponse createUser(UserCreateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.createUser(request));
    }

    public UserProfileResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateUser(userId, request));
    }

    public UserProfileResponse updateStatus(Long userId, UserStatusUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateStatus(userId, request));
    }

    public RoleListResponse updateAuthorization(Long userId, UserAuthorizationUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateAuthorization(userId, request));
    }

    public PermissionListResponse refreshPermissions(Long userId) {
        return remoteCallSupport.unwrap(userServiceClient.refreshPermissions(userId));
    }

    public void resetPassword(Long userId, AdminPasswordResetRequest request) {
        remoteCallSupport.ensureOk(userServiceClient.resetPassword(userId, request));
    }

    public PagedResult<AdminRoleSummaryResponse> listRoles(String roleName, Integer pageNum, Integer pageSize) {
        return remoteCallSupport.unwrap(userServiceClient.listRoles(roleName, pageNum, pageSize));
    }

    public List<RolePermissionDefinitionResponse> listPermissionCatalog(String permissionType) {
        return remoteCallSupport.unwrap(userServiceClient.listPermissionCatalog(permissionType));
    }

    public AdminRoleSummaryResponse createRole(AdminRoleCreateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.createRole(request));
    }

    public AdminRoleSummaryResponse updateRole(Long roleId, AdminRoleUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateRole(roleId, request));
    }

    public AdminRoleSummaryResponse allocateRolePermissions(Long roleId, AdminRolePermissionAllocateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.allocateRolePermissions(roleId, request));
    }

    public void deleteRole(Long roleId) {
        remoteCallSupport.ensureOk(userServiceClient.deleteRole(roleId));
    }

    public PagedResult<AdminDepartmentSummaryResponse> listDepartments(String deptName,
                                                                       Long parentDepartmentId,
                                                                       Integer pageNum,
                                                                       Integer pageSize) {
        return remoteCallSupport.unwrap(userServiceClient.listDepartments(deptName, parentDepartmentId, pageNum, pageSize));
    }

    public List<OrganizationTreeNodeResponse> getOrganizationTree() {
        return remoteCallSupport.unwrap(userServiceClient.getOrganizationTree());
    }

    public AdminDepartmentSummaryResponse createDepartment(AdminDepartmentCreateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.createDepartment(request));
    }

    public AdminDepartmentSummaryResponse updateDepartment(Long departmentId, AdminDepartmentUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateDepartment(departmentId, request));
    }

    public void deleteDepartment(Long departmentId) {
        remoteCallSupport.ensureOk(userServiceClient.deleteDepartment(departmentId));
    }

    public PagedResult<DepartmentUserSummaryResponse> listDepartmentUsers(Long departmentId,
                                                                          String account,
                                                                          String userName,
                                                                          Integer pageNum,
                                                                          Integer pageSize) {
        return remoteCallSupport.unwrap(userServiceClient.listDepartmentUsers(
                departmentId, account, userName, pageNum, pageSize
        ));
    }

    public List<DepartmentAttributeDefinitionResponse> listAttributeDefinitions(Long departmentId) {
        return remoteCallSupport.unwrap(userServiceClient.listAttributeDefinitions(departmentId));
    }

    public DepartmentAttributeDefinitionResponse createAttributeDefinition(Long departmentId,
                                                                          DepartmentAttributeDefinitionCreateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.createAttributeDefinition(departmentId, request));
    }

    public void deleteAttributeDefinition(Long departmentId, String attributeKey) {
        remoteCallSupport.ensureOk(userServiceClient.deleteAttributeDefinition(departmentId, attributeKey));
    }

    public AdminDepartmentSummaryResponse updateDepartmentAttributes(Long departmentId, DepartmentAttributesUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateDepartmentAttributes(departmentId, request));
    }

    public PagedResult<DepartmentUserSummaryResponse> addMembers(Long departmentId, DepartmentMemberRelationRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.addMembers(departmentId, request));
    }

    public DepartmentMembershipResponse removeMember(Long departmentId, Long userId) {
        return remoteCallSupport.unwrap(userServiceClient.removeMember(departmentId, userId));
    }

    public List<DepartmentMembershipResponse> transferMembers(DepartmentTransferRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.transferMembers(request));
    }

    public DepartmentMembershipResponse getUserMembership(Long userId) {
        return remoteCallSupport.unwrap(userServiceClient.getUserMembership(userId));
    }

    public DepartmentMembershipResponse updateMemberAttributes(Long departmentId,
                                                               Long userId,
                                                               DepartmentMemberAttributesUpdateRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.updateMemberAttributes(departmentId, userId, request));
    }

    public DepartmentMemberAttributesBatchResultResponse batchUpdateMemberAttributes(Long departmentId,
                                                                                     DepartmentMemberAttributesBatchRequest request) {
        return remoteCallSupport.unwrap(userServiceClient.batchUpdateMemberAttributes(departmentId, request));
    }
}
