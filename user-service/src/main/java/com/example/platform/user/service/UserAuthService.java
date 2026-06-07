package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.domain.UserSession;
import com.example.platform.user.dto.AuthLoginRequest;
import com.example.platform.user.dto.AuthLoginResponse;
import com.example.platform.user.dto.UserRegistrationRequest;
import com.example.platform.user.dto.UserRegistrationResponse;
import com.example.platform.user.dto.VerifyCodeSendRequest;
import com.example.platform.user.dto.VerifyCodeSendResponse;
import com.example.platform.user.repository.LoginAttemptRepository;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAuthService {

    private static final Long DEFAULT_REGISTER_DEPARTMENT_ID = 10L;

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserPasswordPolicyService userPasswordPolicyService;
    private final UserViewAssembler userViewAssembler;
    private final UserAccessSupport userAccessSupport;
    private final UserVerificationCodeService userVerificationCodeService;
    private final com.example.platform.user.config.UserServiceProperties userServiceProperties;

    public UserAuthService(UserAccountRepository userAccountRepository,
                           UserSessionRepository userSessionRepository,
                           LoginAttemptRepository loginAttemptRepository,
                           UserPasswordPolicyService userPasswordPolicyService,
                           UserViewAssembler userViewAssembler,
                           UserAccessSupport userAccessSupport,
                           UserVerificationCodeService userVerificationCodeService,
                           com.example.platform.user.config.UserServiceProperties userServiceProperties) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.userPasswordPolicyService = userPasswordPolicyService;
        this.userViewAssembler = userViewAssembler;
        this.userAccessSupport = userAccessSupport;
        this.userVerificationCodeService = userVerificationCodeService;
        this.userServiceProperties = userServiceProperties;
    }

    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if (!Boolean.TRUE.equals(request.agreeProtocol())) {
            throw new BusinessException(
                    "REGISTER_PROTOCOL_REQUIRED",
                    "register protocol must be accepted",
                    HttpStatus.FORBIDDEN
            );
        }
        if (userAccountRepository.findByAccount(request.account()).isPresent()) {
            throw new BusinessException("ACCOUNT_ALREADY_EXISTS", "account already exists");
        }
        userVerificationCodeService.verifyOrThrow(
                request.account(),
                request.contact(),
                UserVerificationCodeService.SCENE_REGISTER,
                request.verifyCode()
        );

        userPasswordPolicyService.validateStrength(request.password());
        String email = resolveEmail(request.account(), request.contact());
        String phone = resolvePhone(request.account(), request.contact());
        ensureUniqueContact(email, phone, null);
        UserAccount created = userAccountRepository.create(
                request.account(),
                userPasswordPolicyService.encode(request.password()),
                request.userName().trim(),
                email,
                phone,
                null,
                DEFAULT_REGISTER_DEPARTMENT_ID,
                List.of("USER"),
                List.of(),
                request.extFields()
        );
        userVerificationCodeService.consume(request.account(), UserVerificationCodeService.SCENE_REGISTER);
        return new UserRegistrationResponse(created.userId(), created.account(), created.status());
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        String loginType = request.loginType() == null || request.loginType().isBlank()
                ? "password"
                : request.loginType().trim();
        if (!List.of("password", "thirdParty").contains(loginType)) {
            throw new BusinessException("INVALID_LOGIN_TYPE", "loginType must be password or thirdParty");
        }

        UserAccount user = userAccountRepository.findByAccount(request.account())
                .orElseThrow(userPasswordPolicyService::invalidCredentials);
        userAccessSupport.ensureLoginAllowed(user);

        if ("password".equals(loginType)) {
            if (request.password() == null || request.password().isBlank()) {
                throw new BusinessException("PASSWORD_REQUIRED", "password is required");
            }
            if (!userPasswordPolicyService.matches(request.password(), user.password())) {
                int failedCount = loginAttemptRepository.recordFailure(user.account());
                if (failedCount >= userServiceProperties.loginFailureFreezeThreshold()) {
                    userAccountRepository.updateStatus(user.userId(), "FROZEN");
                    userSessionRepository.removeByUserId(user.userId());
                    throw new BusinessException(
                            "ACCOUNT_FROZEN",
                            "account has been frozen after too many failed login attempts",
                            HttpStatus.UNAUTHORIZED
                    );
                }
                throw userPasswordPolicyService.invalidCredentials();
            }
        }
        if ("thirdParty".equals(loginType)
                && (request.thirdPartyInfo() == null || request.thirdPartyInfo().isEmpty())) {
            throw new BusinessException("THIRD_PARTY_INFO_REQUIRED", "thirdPartyInfo is required");
        }

        loginAttemptRepository.clear(user.account());
        UserSession session = userSessionRepository.create(user.userId());
        return new AuthLoginResponse(
                user.userId(),
                user.account(),
                user.userName(),
                user.roles(),
                userViewAssembler.effectivePermissions(user),
                session.sessionKey(),
                session.expiresAt()
        );
    }

    public void logout(String sessionKey) {
        userSessionRepository.remove(sessionKey);
    }

    public VerifyCodeSendResponse sendVerifyCode(VerifyCodeSendRequest request) {
        var code = userVerificationCodeService.send(request.account(), request.contact(), request.scene());
        return new VerifyCodeSendResponse(code.account(), code.contact(), code.scene(), code.expiresAt());
    }

    private void ensureUniqueContact(String email, String phone, Long excludedUserId) {
        if (userAccountRepository.existsByEmail(email, excludedUserId)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "email already exists");
        }
        if (userAccountRepository.existsByPhone(phone, excludedUserId)) {
            throw new BusinessException("PHONE_ALREADY_EXISTS", "phone already exists");
        }
    }

    private String resolveEmail(String account, String contact) {
        if (isEmail(account)) {
            return account.trim();
        }
        if (isEmail(contact)) {
            return contact.trim();
        }
        return account.trim() + "@example.local";
    }

    private String resolvePhone(String account, String contact) {
        if (isPhone(account)) {
            return account.trim();
        }
        if (isPhone(contact)) {
            return contact.trim();
        }
        return "13000000000";
    }

    private boolean isEmail(String value) {
        return value != null && value.contains("@");
    }

    private boolean isPhone(String value) {
        return value != null && value.matches("\\d{11}");
    }
}
