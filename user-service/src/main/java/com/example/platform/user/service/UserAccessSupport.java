package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.repository.DepartmentRepository;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UserAccessSupport {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserViewAssembler userViewAssembler;
    private final DepartmentRepository departmentRepository;

    public UserAccessSupport(UserAccountRepository userAccountRepository,
                             UserSessionRepository userSessionRepository,
                             UserViewAssembler userViewAssembler,
                             DepartmentRepository departmentRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.userViewAssembler = userViewAssembler;
        this.departmentRepository = departmentRepository;
    }

    public UserAccount requireCurrentUser(Long userId, String sessionKey) {
        Long resolvedUserId = userSessionRepository.findBySessionKey(sessionKey)
                .map(session -> session.userId())
                .orElse(userId);
        if (resolvedUserId == null) {
            throw new BusinessException("UNAUTHORIZED", "login required", HttpStatus.UNAUTHORIZED);
        }
        return requireUser(resolvedUserId);
    }

    public UserAccount requireUser(Long userId) {
        return userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
    }

    public UserAccount requireAdmin(Long userId, String sessionKey, String permissionCode) {
        UserAccount operator = requireCurrentUser(userId, sessionKey);
        if (!userViewAssembler.effectivePermissions(operator).contains(permissionCode)) {
            throw new BusinessException("FORBIDDEN", "permission denied", HttpStatus.FORBIDDEN);
        }
        return operator;
    }

    public UserAccount requireScopedDepartmentAccess(Long userId,
                                                     String sessionKey,
                                                     String permissionCode,
                                                     Long targetDepartmentId) {
        UserAccount operator = requireAdmin(userId, sessionKey, permissionCode);
        ensureDepartmentAccessible(operator, targetDepartmentId);
        return operator;
    }

    public UserAccount requireScopedUserAccess(Long userId,
                                               String sessionKey,
                                               String permissionCode,
                                               Long targetUserId) {
        UserAccount operator = requireAdmin(userId, sessionKey, permissionCode);
        UserAccount targetUser = requireUser(targetUserId);
        ensureUserAccessible(operator, targetUser);
        return operator;
    }

    public void ensureDepartmentAccessible(UserAccount operator, Long targetDepartmentId) {
        if (targetDepartmentId == null || !isDepartmentScopedOperator(operator)) {
            return;
        }
        if (operator.departmentId() == null
                || !departmentRepository.isDescendantOrSelf(operator.departmentId(), targetDepartmentId)) {
            throw new BusinessException("FORBIDDEN", "department scope exceeded", HttpStatus.FORBIDDEN);
        }
    }

    public void ensureUserAccessible(UserAccount operator, UserAccount targetUser) {
        if (!isDepartmentScopedOperator(operator)) {
            return;
        }
        if (targetUser.departmentId() == null
                || operator.departmentId() == null
                || !departmentRepository.isDescendantOrSelf(operator.departmentId(), targetUser.departmentId())) {
            throw new BusinessException("FORBIDDEN", "user scope exceeded", HttpStatus.FORBIDDEN);
        }
    }

    public boolean isDepartmentScopedOperator(UserAccount operator) {
        return operator.roles().contains("DEPARTMENT_ADMIN") && !operator.roles().contains("ADMIN");
    }

    public void ensureLoginAllowed(UserAccount user) {
        if (!"ENABLED".equals(user.status())) {
            throw new BusinessException(
                    "ACCOUNT_STATUS_INVALID",
                    "account status is " + user.status(),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
}
