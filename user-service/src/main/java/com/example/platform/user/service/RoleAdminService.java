package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.RoleDefinition;
import com.example.platform.user.dto.AdminRoleCreateRequest;
import com.example.platform.user.dto.AdminRolePermissionAllocateRequest;
import com.example.platform.user.dto.AdminRoleSummaryResponse;
import com.example.platform.user.dto.AdminRoleUpdateRequest;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.RolePermissionDefinitionResponse;
import com.example.platform.user.repository.PermissionCatalogRepository;
import com.example.platform.user.repository.RoleRepository;
import com.example.platform.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RoleAdminService {

    private final RoleRepository roleRepository;
    private final PermissionCatalogRepository permissionCatalogRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserViewAssembler userViewAssembler;
    private final UserAccessSupport userAccessSupport;

    public RoleAdminService(RoleRepository roleRepository,
                            PermissionCatalogRepository permissionCatalogRepository,
                            UserAccountRepository userAccountRepository,
                            UserViewAssembler userViewAssembler,
                            UserAccessSupport userAccessSupport) {
        this.roleRepository = roleRepository;
        this.permissionCatalogRepository = permissionCatalogRepository;
        this.userAccountRepository = userAccountRepository;
        this.userViewAssembler = userViewAssembler;
        this.userAccessSupport = userAccessSupport;
    }

    public PagedResult<AdminRoleSummaryResponse> listRoles(Long operatorUserId,
                                                           String sessionKey,
                                                           String roleName,
                                                           Integer pageNum,
                                                           Integer pageSize) {
        userAccessSupport.requireAdmin(operatorUserId, sessionKey, "role:read");
        List<AdminRoleSummaryResponse> items = roleRepository.findAll().stream()
                .filter(role -> roleName == null
                        || roleName.isBlank()
                        || role.roleName().toLowerCase(Locale.ROOT).contains(roleName.trim().toLowerCase(Locale.ROOT))
                        || role.roleCode().toLowerCase(Locale.ROOT).contains(roleName.trim().toLowerCase(Locale.ROOT)))
                .map(userViewAssembler::toRoleSummary)
                .toList();
        return page(items, pageNum, pageSize);
    }

    public List<RolePermissionDefinitionResponse> listPermissions(Long operatorUserId,
                                                                  String sessionKey,
                                                                  String permissionType) {
        userAccessSupport.requireAdmin(operatorUserId, sessionKey, "role:read");
        return permissionCatalogRepository.findAll(permissionType).stream()
                .map(userViewAssembler::toPermissionDefinition)
                .toList();
    }

    public AdminRoleSummaryResponse createRole(Long operatorUserId,
                                               String sessionKey,
                                               AdminRoleCreateRequest request) {
        userAccessSupport.requireAdmin(operatorUserId, sessionKey, "role:write");
        if (roleRepository.findByCode(request.roleCode()).isPresent()) {
            throw new BusinessException("ROLE_ALREADY_EXISTS", "role already exists");
        }
        List<String> permissions = request.permissions() == null ? List.of() : List.copyOf(request.permissions());
        validatePermissions(permissions);
        RoleDefinition created = roleRepository.create(
                request.roleCode().trim(),
                request.roleName().trim(),
                request.description(),
                permissions
        );
        return userViewAssembler.toRoleSummary(created);
    }

    public AdminRoleSummaryResponse updateRole(Long operatorUserId,
                                               String sessionKey,
                                               Long roleId,
                                               AdminRoleUpdateRequest request) {
        userAccessSupport.requireAdmin(operatorUserId, sessionKey, "role:write");
        RoleDefinition existing = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "role not found", HttpStatus.NOT_FOUND));
        List<String> permissions = request.permissions() == null ? existing.permissions() : List.copyOf(request.permissions());
        validatePermissions(permissions);
        RoleDefinition updated = roleRepository.update(roleId, request.roleName(), request.description(), permissions)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "role not found", HttpStatus.NOT_FOUND));
        return userViewAssembler.toRoleSummary(updated);
    }

    public AdminRoleSummaryResponse allocatePermissions(Long operatorUserId,
                                                        String sessionKey,
                                                        Long roleId,
                                                        AdminRolePermissionAllocateRequest request) {
        userAccessSupport.requireAdmin(operatorUserId, sessionKey, "role:write");
        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "role not found", HttpStatus.NOT_FOUND));
        List<String> permissions = request.permissions() == null ? List.of() : List.copyOf(request.permissions());
        validatePermissions(permissions);
        RoleDefinition updated = roleRepository.updatePermissions(roleId, permissions)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "role not found", HttpStatus.NOT_FOUND));
        return userViewAssembler.toRoleSummary(updated);
    }

    public void deleteRole(Long operatorUserId, String sessionKey, Long roleId) {
        userAccessSupport.requireAdmin(operatorUserId, sessionKey, "role:write");
        RoleDefinition existing = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "role not found", HttpStatus.NOT_FOUND));
        long boundUserCount = userAccountRepository.countByRole(existing.roleCode());
        if (boundUserCount > 0) {
            throw new BusinessException("ROLE_IN_USE", "role is bound by " + boundUserCount + " users");
        }
        roleRepository.delete(roleId);
    }

    private void validatePermissions(List<String> permissions) {
        if (!permissionCatalogRepository.existsAll(permissions)) {
            throw new BusinessException("PERMISSION_NOT_FOUND", "permission does not exist");
        }
    }

    private <T> PagedResult<T> page(List<T> items, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, items.size());
        int toIndex = Math.min(fromIndex + safePageSize, items.size());
        return new PagedResult<>(items.size(), items.subList(fromIndex, toIndex));
    }
}
