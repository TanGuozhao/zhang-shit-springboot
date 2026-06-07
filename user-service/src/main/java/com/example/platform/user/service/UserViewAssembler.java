package com.example.platform.user.service;

import com.example.platform.user.domain.Department;
import com.example.platform.user.domain.DepartmentAttributeDefinition;
import com.example.platform.user.domain.PermissionDefinition;
import com.example.platform.user.domain.RoleDefinition;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.domain.UserProfileModificationRecord;
import com.example.platform.user.dto.AdminDepartmentSummaryResponse;
import com.example.platform.user.dto.AdminRoleSummaryResponse;
import com.example.platform.user.dto.AdminUserSummaryResponse;
import com.example.platform.user.dto.DepartmentAttributeDefinitionResponse;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.dto.DepartmentUserSummaryResponse;
import com.example.platform.user.dto.OrganizationTreeNodeResponse;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.RolePermissionDefinitionResponse;
import com.example.platform.user.dto.UserProfileModificationRecordResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusResponse;
import com.example.platform.user.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class UserViewAssembler {

    private final RoleRepository roleRepository;

    public UserViewAssembler(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.email(),
                user.phone(),
                user.avatar(),
                user.status(),
                statusDesc(user.status()),
                user.departmentId(),
                user.roles(),
                effectivePermissions(user),
                user.extFields(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    public PermissionListResponse toPermissionList(UserAccount user) {
        return new PermissionListResponse(user.userId(), effectivePermissions(user));
    }

    public RoleListResponse toRoleList(UserAccount user) {
        return new RoleListResponse(user.userId(), user.roles());
    }

    public UserStatusResponse toStatus(UserAccount user) {
        return new UserStatusResponse(user.status(), statusDesc(user.status()));
    }

    public AdminUserSummaryResponse toAdminUserSummary(UserAccount user) {
        return new AdminUserSummaryResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.status(),
                user.departmentId(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    public AdminRoleSummaryResponse toRoleSummary(RoleDefinition role) {
        return new AdminRoleSummaryResponse(
                role.roleId(),
                role.roleCode(),
                role.roleName(),
                role.description(),
                role.permissions(),
                role.createdAt(),
                role.updatedAt()
        );
    }

    public RolePermissionDefinitionResponse toPermissionDefinition(PermissionDefinition permission) {
        return new RolePermissionDefinitionResponse(
                permission.permissionCode(),
                permission.permissionName(),
                permission.permissionType(),
                permission.description()
        );
    }

    public DepartmentResponse toDepartmentResponse(Department department) {
        return new DepartmentResponse(
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                department.parentDepartmentId(),
                department.description(),
                department.attributes(),
                department.createdAt(),
                department.updatedAt()
        );
    }

    public AdminDepartmentSummaryResponse toDepartmentSummary(Department department) {
        return new AdminDepartmentSummaryResponse(
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                department.parentDepartmentId(),
                department.description(),
                department.attributes(),
                department.createdAt(),
                department.updatedAt()
        );
    }

    public DepartmentUserSummaryResponse toDepartmentUserSummary(UserAccount user) {
        return new DepartmentUserSummaryResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.status()
        );
    }

    public DepartmentAttributeDefinitionResponse toDepartmentAttributeDefinition(DepartmentAttributeDefinition definition) {
        return new DepartmentAttributeDefinitionResponse(
                definition.attributeId(),
                definition.departmentId(),
                definition.attributeKey(),
                definition.attributeName(),
                definition.dataType(),
                definition.defaultValue(),
                definition.required(),
                definition.rules(),
                definition.displayOrder(),
                definition.createdAt(),
                definition.updatedAt()
        );
    }

    public OrganizationTreeNodeResponse toOrganizationTreeNode(Department department,
                                                               int memberCount,
                                                               List<DepartmentAttributeDefinitionResponse> definitions,
                                                               List<OrganizationTreeNodeResponse> children) {
        return new OrganizationTreeNodeResponse(
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                department.parentDepartmentId(),
                department.description(),
                memberCount,
                department.attributes(),
                definitions,
                children
        );
    }

    public UserProfileModificationRecordResponse toModificationRecord(UserProfileModificationRecord record) {
        return new UserProfileModificationRecordResponse(
                record.modifyTime(),
                record.modifyField(),
                record.oldValue(),
                record.newValue()
        );
    }

    public List<String> effectivePermissions(UserAccount user) {
        List<String> roles = user.roles() == null ? List.of() : user.roles();
        List<String> directPermissions = user.permissions() == null ? List.of() : user.permissions();
        Set<String> merged = new LinkedHashSet<>(roleRepository.permissionsForRoles(roles));
        merged.addAll(directPermissions);
        return List.copyOf(merged);
    }

    public String statusDesc(String status) {
        return switch (status) {
            case "ENABLED" -> "Account enabled";
            case "DISABLED" -> "Account disabled";
            case "FROZEN" -> "Account frozen";
            case "CANCEL_PENDING" -> "Account cancellation pending";
            default -> "Unknown status";
        };
    }
}
