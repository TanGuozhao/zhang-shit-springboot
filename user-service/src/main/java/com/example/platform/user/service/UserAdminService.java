package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserAuthorizationUpdateRequest;
import com.example.platform.user.dto.UserCreateRequest;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import com.example.platform.user.repository.DepartmentRepository;
import com.example.platform.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAdminService {

    private final UserAccountRepository userAccountRepository;
    private final DepartmentRepository departmentRepository;

    public UserAdminService(UserAccountRepository userAccountRepository,
                            DepartmentRepository departmentRepository) {
        this.userAccountRepository = userAccountRepository;
        this.departmentRepository = departmentRepository;
    }

    public UserProfileResponse createUser(UserCreateRequest request) {
        if (userAccountRepository.findByAccount(request.account()).isPresent()) {
            throw new BusinessException("ACCOUNT_ALREADY_EXISTS", "account already exists");
        }
        departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "department not found"));

        UserAccount created = userAccountRepository.create(
                request.account(),
                request.password(),
                request.userName(),
                request.email(),
                request.phone(),
                request.departmentId(),
                request.roles(),
                request.permissions()
        );
        return toProfile(created);
    }

    public UserProfileResponse updateStatus(Long userId, UserStatusUpdateRequest request) {
        String normalizedStatus = request.status().trim().toUpperCase();
        if (!List.of("ENABLED", "DISABLED").contains(normalizedStatus)) {
            throw new BusinessException("INVALID_STATUS", "status must be ENABLED or DISABLED");
        }
        UserAccount updated = userAccountRepository.updateStatus(userId, normalizedStatus)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        return toProfile(updated);
    }

    public RoleListResponse updateAuthorization(Long userId, UserAuthorizationUpdateRequest request) {
        UserAccount updated = userAccountRepository.updateAuthorization(userId, request.roles(), request.permissions())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        return new RoleListResponse(updated.userId(), updated.roles());
    }

    public PermissionListResponse getUpdatedPermissions(Long userId) {
        UserAccount user = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        return new PermissionListResponse(user.userId(), user.permissions());
    }

    private UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.email(),
                user.phone(),
                user.status(),
                user.departmentId(),
                user.roles(),
                user.permissions()
        );
    }
}
