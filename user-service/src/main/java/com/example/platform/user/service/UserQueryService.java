package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;

    public UserQueryService(UserAccountRepository userAccountRepository,
                            UserSessionRepository userSessionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public UserProfileResponse getCurrentUser(Long userId, String sessionKey) {
        Long resolvedUserId = userSessionRepository.findBySessionKey(sessionKey)
                .map(session -> session.userId())
                .orElse(userId == null ? 1001L : userId);
        return getUser(resolvedUserId);
    }

    public UserProfileResponse getUser(Long userId) {
        UserAccount user = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
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

    public PermissionListResponse getPermissions(Long userId) {
        UserAccount user = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        return new PermissionListResponse(userId, user.permissions());
    }

    public RoleListResponse getRoles(Long userId) {
        UserAccount user = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        return new RoleListResponse(userId, user.roles());
    }
}
