package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.config.UserServiceProperties;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.AdminPasswordResetRequest;
import com.example.platform.user.dto.AdminUserSummaryResponse;
import com.example.platform.user.dto.AdminUserUpdateRequest;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserAuthorizationUpdateRequest;
import com.example.platform.user.dto.UserCreateRequest;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import com.example.platform.user.repository.DepartmentRepository;
import com.example.platform.user.repository.PermissionCatalogRepository;
import com.example.platform.user.repository.RoleRepository;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UserAdminService {

    private final UserAccountRepository userAccountRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PermissionCatalogRepository permissionCatalogRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserPasswordPolicyService userPasswordPolicyService;
    private final UserViewAssembler userViewAssembler;
    private final UserAccessSupport userAccessSupport;
    private final UserServiceProperties userServiceProperties;

    public UserAdminService(UserAccountRepository userAccountRepository,
                            DepartmentRepository departmentRepository,
                            RoleRepository roleRepository,
                            PermissionCatalogRepository permissionCatalogRepository,
                            UserSessionRepository userSessionRepository,
                            UserPasswordPolicyService userPasswordPolicyService,
                            UserViewAssembler userViewAssembler,
                            UserAccessSupport userAccessSupport,
                            UserServiceProperties userServiceProperties) {
        this.userAccountRepository = userAccountRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.permissionCatalogRepository = permissionCatalogRepository;
        this.userSessionRepository = userSessionRepository;
        this.userPasswordPolicyService = userPasswordPolicyService;
        this.userViewAssembler = userViewAssembler;
        this.userAccessSupport = userAccessSupport;
        this.userServiceProperties = userServiceProperties;
    }

    public PagedResult<AdminUserSummaryResponse> listUsers(Long operatorUserId,
                                                           String sessionKey,
                                                           String account,
                                                           String userName,
                                                           String status,
                                                           Integer pageNum,
                                                           Integer pageSize) {
        UserAccount operator = userAccessSupport.requireAdmin(operatorUserId, sessionKey, "user:read");
        List<AdminUserSummaryResponse> items = userAccountRepository.findAll().stream()
                .filter(user -> canAccess(operator, user))
                .filter(user -> matches(user.account(), account))
                .filter(user -> matches(user.userName(), userName))
                .filter(user -> matchesExact(user.status(), status))
                .map(userViewAssembler::toAdminUserSummary)
                .toList();
        return page(items, pageNum, pageSize);
    }

    public UserProfileResponse createUser(Long operatorUserId,
                                          String sessionKey,
                                          UserCreateRequest request) {
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "user:write", request.departmentId());
        if (userAccountRepository.findByAccount(request.account()).isPresent()) {
            throw new BusinessException("ACCOUNT_ALREADY_EXISTS", "account already exists");
        }

        userPasswordPolicyService.validateStrength(request.password());
        validateDepartment(request.departmentId());
        ensureUniqueContact(normalizeEmail(request.email(), request.account()), request.phone(), null);

        List<String> roles = request.roles() == null || request.roles().isEmpty()
                ? List.of("USER")
                : List.copyOf(request.roles());
        List<String> permissions = request.permissions() == null ? List.of() : List.copyOf(request.permissions());
        validateRolesAndPermissions(roles, permissions);

        UserAccount created = userAccountRepository.create(
                request.account(),
                userPasswordPolicyService.encode(request.password()),
                request.userName().trim(),
                normalizeEmail(request.email(), request.account()),
                request.phone().trim(),
                normalizeNullable(request.avatar()),
                request.departmentId(),
                roles,
                permissions,
                request.extFields()
        );
        return userViewAssembler.toProfile(created);
    }

    public UserProfileResponse updateUser(Long operatorUserId,
                                          String sessionKey,
                                          Long userId,
                                          AdminUserUpdateRequest request) {
        userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "user:write", userId);
        UserAccount existing = userAccessSupport.requireUser(userId);
        userAccessSupport.requireScopedDepartmentAccess(operatorUserId, sessionKey, "user:write", request.departmentId());
        validateDepartment(request.departmentId());

        List<String> roles = request.roles() == null || request.roles().isEmpty()
                ? existing.roles()
                : List.copyOf(request.roles());
        List<String> permissions = request.permissions() == null
                ? existing.permissions()
                : List.copyOf(request.permissions());
        validateRolesAndPermissions(roles, permissions);

        String nextEmail = normalizeEmail(request.email(), existing.account());
        String nextPhone = request.phone().trim();
        ensureUniqueContact(nextEmail, nextPhone, existing.userId());

        UserAccount updated = userAccountRepository.updateUser(
                userId,
                request.userName().trim(),
                nextEmail,
                nextPhone,
                request.avatar() == null || request.avatar().isBlank() ? existing.avatar() : request.avatar().trim(),
                request.departmentId(),
                roles,
                permissions,
                request.extFields() == null ? existing.extFields() : request.extFields()
        ).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        return userViewAssembler.toProfile(updated);
    }

    public UserProfileResponse updateStatus(Long operatorUserId,
                                            String sessionKey,
                                            Long userId,
                                            UserStatusUpdateRequest request) {
        userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "user:status", userId);
        String normalizedStatus = request.status().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ENABLED", "DISABLED", "FROZEN", "CANCEL_PENDING").contains(normalizedStatus)) {
            throw new BusinessException("INVALID_STATUS", "status must be ENABLED, DISABLED, FROZEN or CANCEL_PENDING");
        }
        UserAccount updated = userAccountRepository.updateStatus(userId, normalizedStatus)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        if (!"ENABLED".equals(normalizedStatus)) {
            userSessionRepository.removeByUserId(userId);
        }
        return userViewAssembler.toProfile(updated);
    }

    public RoleListResponse updateAuthorization(Long operatorUserId,
                                                String sessionKey,
                                                Long userId,
                                                UserAuthorizationUpdateRequest request) {
        userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "user:write", userId);
        UserAccount existing = userAccessSupport.requireUser(userId);
        List<String> roles = request.roles() == null || request.roles().isEmpty()
                ? existing.roles()
                : List.copyOf(request.roles());
        List<String> permissions = request.permissions() == null
                ? existing.permissions()
                : List.copyOf(request.permissions());
        validateRolesAndPermissions(roles, permissions);

        UserAccount updated = userAccountRepository.updateAuthorization(userId, roles, permissions)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        return userViewAssembler.toRoleList(updated);
    }

    public PermissionListResponse getUpdatedPermissions(Long operatorUserId,
                                                        String sessionKey,
                                                        Long userId) {
        userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "user:read", userId);
        return userViewAssembler.toPermissionList(userAccessSupport.requireUser(userId));
    }

    public void resetPassword(Long operatorUserId,
                              String sessionKey,
                              Long userId,
                              AdminPasswordResetRequest request) {
        userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "user:write", userId);
        UserAccount existing = userAccessSupport.requireUser(userId);
        userPasswordPolicyService.validateStrength(request.newPassword());
        String encodedPassword = userPasswordPolicyService.encode(request.newPassword());
        if (userAccountRepository.passwordUsedRecently(existing.userId(), request.newPassword(), userServiceProperties.passwordHistoryLimit())) {
            throw new BusinessException("PASSWORD_REUSE_FORBIDDEN", "password was used recently");
        }
        userAccountRepository.updatePassword(userId, encodedPassword)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        userSessionRepository.removeByUserId(userId);
    }

    private void ensureUniqueContact(String email, String phone, Long excludedUserId) {
        if (userAccountRepository.existsByEmail(email, excludedUserId)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "email already exists");
        }
        if (userAccountRepository.existsByPhone(phone, excludedUserId)) {
            throw new BusinessException("PHONE_ALREADY_EXISTS", "phone already exists");
        }
    }

    private void validateDepartment(Long departmentId) {
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found", HttpStatus.NOT_FOUND));
    }

    private void validateRolesAndPermissions(List<String> roles, List<String> permissions) {
        if (!roleRepository.existsAll(roles)) {
            throw new BusinessException("ROLE_NOT_FOUND", "role does not exist");
        }
        if (!permissionCatalogRepository.existsAll(permissions)) {
            throw new BusinessException("PERMISSION_NOT_FOUND", "permission does not exist");
        }
    }

    private boolean canAccess(UserAccount operator, UserAccount target) {
        if (!userAccessSupport.isDepartmentScopedOperator(operator)) {
            return true;
        }
        return target.departmentId() != null
                && operator.departmentId() != null
                && departmentRepository.isDescendantOrSelf(operator.departmentId(), target.departmentId());
    }

    private String normalizeEmail(String email, String account) {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (account != null && account.contains("@")) {
            return account.trim();
        }
        return account + "@example.local";
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean matches(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesExact(String value, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return value != null && value.equalsIgnoreCase(expected.trim());
    }

    private <T> PagedResult<T> page(List<T> items, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, items.size());
        int toIndex = Math.min(fromIndex + safePageSize, items.size());
        return new PagedResult<>(items.size(), items.subList(fromIndex, toIndex));
    }
}
