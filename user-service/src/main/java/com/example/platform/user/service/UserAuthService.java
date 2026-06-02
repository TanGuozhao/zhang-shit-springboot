package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.AuthLoginRequest;
import com.example.platform.user.dto.AuthLoginResponse;
import com.example.platform.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService {

    private final UserAccountRepository userAccountRepository;

    public UserAuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountRepository.findByAccount(request.account())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found"));
        if (!user.password().equals(request.password())) {
            throw new BusinessException("INVALID_CREDENTIALS", "invalid credentials");
        }
        return new AuthLoginResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.roles(),
                user.permissions(),
                "SESSION-" + user.userId()
        );
    }
}
