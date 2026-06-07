package com.example.platform.user.service;

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
    private final UserAccessSupport userAccessSupport;
    private final UserViewAssembler userViewAssembler;

    public UserQueryService(UserAccountRepository userAccountRepository,
                            UserSessionRepository userSessionRepository,
                            UserAccessSupport userAccessSupport,
                            UserViewAssembler userViewAssembler) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.userAccessSupport = userAccessSupport;
        this.userViewAssembler = userViewAssembler;
    }

    public UserProfileResponse getCurrentUser(Long userId, String sessionKey) {
        return userViewAssembler.toProfile(userAccessSupport.requireCurrentUser(userId, sessionKey));
    }

    public UserProfileResponse getUser(Long operatorUserId, String sessionKey, Long userId) {
        return userViewAssembler.toProfile(requireReadableUser(operatorUserId, sessionKey, userId));
    }

    public PermissionListResponse getPermissions(Long operatorUserId, String sessionKey, Long userId) {
        UserAccount user = requireReadableUser(operatorUserId, sessionKey, userId);
        return userViewAssembler.toPermissionList(user);
    }

    public RoleListResponse getRoles(Long operatorUserId, String sessionKey, Long userId) {
        UserAccount user = requireReadableUser(operatorUserId, sessionKey, userId);
        return userViewAssembler.toRoleList(user);
    }

    private UserAccount requireReadableUser(Long operatorUserId, String sessionKey, Long targetUserId) {
        UserAccount currentUser = userAccessSupport.requireCurrentUser(operatorUserId, sessionKey);
        if (currentUser.userId().equals(targetUserId)) {
            return currentUser;
        }
        userAccessSupport.requireScopedUserAccess(operatorUserId, sessionKey, "user:read", targetUserId);
        return userAccessSupport.requireUser(targetUserId);
    }
}
