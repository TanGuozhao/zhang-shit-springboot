package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.domain.UserSession;
import com.example.platform.user.dto.AuthLoginRequest;
import com.example.platform.user.dto.AuthLoginResponse;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;

    public UserAuthService(UserAccountRepository userAccountRepository,
                           UserSessionRepository userSessionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        if (!user.password().equals(request.password())) {
            throw new BusinessException("INVALID_CREDENTIALS", "invalid credentials");
        }
        UserSession session = userSessionRepository.create(user.userId());
        return new AuthLoginResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.roles(),
                user.permissions(),
                session.sessionKey()
        );
    }

    public void logout(String sessionKey) {
        userSessionRepository.remove(sessionKey);
    }
}
