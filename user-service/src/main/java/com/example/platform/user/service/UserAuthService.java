package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.domain.UserSession;
import com.example.platform.user.dto.AuthLoginRequest;
import com.example.platform.user.dto.AuthLoginResponse;
import com.example.platform.user.dto.EmailCodeLoginRequest;
import com.example.platform.user.dto.EmailLoginSendCodeRequest;
import com.example.platform.user.dto.EmailLoginSendCodeResponse;
import com.example.platform.user.dto.ThirdPartyLoginRequest;
import com.example.platform.user.dto.UserRegistrationRequest;
import com.example.platform.user.dto.UserRegistrationResponse;
import com.example.platform.user.dto.VerifyCodeSendRequest;
import com.example.platform.user.dto.VerifyCodeSendResponse;
import com.example.platform.user.repository.ExternalIdentityBindingRepository;
import com.example.platform.user.repository.LoginAttemptRepository;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class UserAuthService {

    private static final Long DEFAULT_REGISTER_DEPARTMENT_ID = 10L;

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final ExternalIdentityBindingRepository externalIdentityBindingRepository;
    private final UserPasswordPolicyService userPasswordPolicyService;
    private final UserViewAssembler userViewAssembler;
    private final UserAccessSupport userAccessSupport;
    private final UserVerificationCodeService userVerificationCodeService;
    private final com.example.platform.user.config.UserServiceProperties userServiceProperties;

    public UserAuthService(UserAccountRepository userAccountRepository,
                           UserSessionRepository userSessionRepository,
                           LoginAttemptRepository loginAttemptRepository,
                           ExternalIdentityBindingRepository externalIdentityBindingRepository,
                           UserPasswordPolicyService userPasswordPolicyService,
                           UserViewAssembler userViewAssembler,
                           UserAccessSupport userAccessSupport,
                           UserVerificationCodeService userVerificationCodeService,
                           com.example.platform.user.config.UserServiceProperties userServiceProperties) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.externalIdentityBindingRepository = externalIdentityBindingRepository;
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
        String loginType = canonicalLoginType(request.loginType());
        if (!List.of("password", "thirdParty").contains(loginType)) {
            throw new BusinessException("INVALID_LOGIN_TYPE", "loginType must be password or thirdParty");
        }
        if ("thirdParty".equals(loginType)) {
            if (request.thirdPartyInfo() == null || request.thirdPartyInfo().isEmpty()) {
                throw new BusinessException("THIRD_PARTY_INFO_REQUIRED", "thirdPartyInfo is required");
            }
            return loginByThirdParty(fromLegacyThirdPartyRequest(request));
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

        return issueLogin(user);
    }

    public EmailLoginSendCodeResponse sendEmailLoginCode(EmailLoginSendCodeRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        var code = userVerificationCodeService.send(
                normalizedEmail,
                normalizedEmail,
                UserVerificationCodeService.SCENE_EMAIL_LOGIN
        );
        return new EmailLoginSendCodeResponse(normalizedEmail, code.expiresAt());
    }

    public AuthLoginResponse loginByEmailCode(EmailCodeLoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        userVerificationCodeService.verifyOrThrow(
                normalizedEmail,
                normalizedEmail,
                UserVerificationCodeService.SCENE_EMAIL_LOGIN,
                request.verifyCode()
        );
        UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
                .or(() -> userAccountRepository.findByAccount(normalizedEmail))
                .orElseGet(() -> autoProvisionEmailUser(normalizedEmail, request));
        userAccessSupport.ensureLoginAllowed(user);
        userVerificationCodeService.consume(normalizedEmail, UserVerificationCodeService.SCENE_EMAIL_LOGIN);
        return issueLogin(user);
    }

    public AuthLoginResponse loginByThirdParty(ThirdPartyLoginRequest request) {
        String provider = normalizeProvider(request.provider());
        UserAccount user = externalIdentityBindingRepository.findByProviderAndSubject(
                        provider,
                        request.providerUserId(),
                        request.providerUnionId()
                )
                .map(binding -> userAccountRepository.findByUserId(binding.userId())
                        .orElseThrow(() -> new BusinessException("BOUND_USER_NOT_FOUND", "bound user not found", HttpStatus.NOT_FOUND)))
                .orElseGet(() -> bindOrProvisionThirdPartyUser(provider, request));
        userAccessSupport.ensureLoginAllowed(user);
        return issueLogin(user);
    }

    public void logout(String sessionKey) {
        userSessionRepository.remove(sessionKey);
    }

    public VerifyCodeSendResponse sendVerifyCode(VerifyCodeSendRequest request) {
        var code = userVerificationCodeService.send(request.account(), request.contact(), request.scene());
        return new VerifyCodeSendResponse(code.account(), code.contact(), code.scene(), code.expiresAt());
    }

    private AuthLoginResponse issueLogin(UserAccount user) {
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

    private UserAccount autoProvisionEmailUser(String normalizedEmail, EmailCodeLoginRequest request) {
        if (!Boolean.TRUE.equals(resolveFlag(request.autoRegister(), userServiceProperties.emailLoginAutoRegister()))) {
            throw new BusinessException("EMAIL_LOGIN_USER_NOT_FOUND", "email is not bound to an account");
        }
        String account = ensureUniqueAccount(normalizedEmail);
        String userName = request.userName() == null || request.userName().isBlank()
                ? deriveUserNameFromEmail(normalizedEmail)
                : request.userName().trim();
        return userAccountRepository.create(
                account,
                userPasswordPolicyService.encode(generateTechnicalPassword("EMAIL")),
                userName,
                normalizedEmail,
                null,
                null,
                DEFAULT_REGISTER_DEPARTMENT_ID,
                List.of("USER"),
                List.of(),
                Map.of("authSource", "EMAIL_CODE")
        );
    }

    private UserAccount bindOrProvisionThirdPartyUser(String provider, ThirdPartyLoginRequest request) {
        UserAccount user = resolveExistingUserForThirdParty(request)
                .orElseGet(() -> autoProvisionThirdPartyUser(provider, request));
        externalIdentityBindingRepository.saveOrUpdate(
                provider,
                request.providerUserId(),
                request.providerUnionId(),
                user.userId(),
                user.account(),
                user.userName(),
                user.email(),
                user.avatar(),
                copyProfile(request.rawProfile(), provider)
        );
        return user;
    }

    private java.util.Optional<UserAccount> resolveExistingUserForThirdParty(ThirdPartyLoginRequest request) {
        String normalizedEmail = normalizeOptionalEmail(request.email());
        if (normalizedEmail != null) {
            return userAccountRepository.findByEmail(normalizedEmail)
                    .or(() -> userAccountRepository.findByAccount(normalizedEmail));
        }
        String normalizedPhone = normalizeOptionalPhone(request.phone());
        if (normalizedPhone != null) {
            return userAccountRepository.findByPhone(normalizedPhone);
        }
        if (request.account() != null && !request.account().isBlank()) {
            return userAccountRepository.findByAccount(request.account());
        }
        return java.util.Optional.empty();
    }

    private UserAccount autoProvisionThirdPartyUser(String provider, ThirdPartyLoginRequest request) {
        if (!Boolean.TRUE.equals(resolveFlag(request.autoRegister(), userServiceProperties.thirdPartyAutoRegister()))) {
            throw new BusinessException("THIRD_PARTY_ACCOUNT_NOT_BOUND", "third-party account is not bound");
        }
        String preferredAccount = request.account();
        if (preferredAccount == null || preferredAccount.isBlank()) {
            preferredAccount = normalizeOptionalEmail(request.email());
        }
        if (preferredAccount == null || preferredAccount.isBlank()) {
            preferredAccount = provider.toLowerCase(Locale.ROOT) + "_" + sanitizeSubject(request.providerUserId());
        }
        String account = ensureUniqueAccount(preferredAccount);
        String email = normalizeOptionalEmail(request.email());
        String phone = normalizeOptionalPhone(request.phone());
        ensureThirdPartyContactCanProvision(email, phone);
        String userName = request.userName() == null || request.userName().isBlank()
                ? provider + " User"
                : request.userName().trim();
        return userAccountRepository.create(
                account,
                userPasswordPolicyService.encode(generateTechnicalPassword(provider)),
                userName,
                email,
                phone,
                blankToNull(request.avatar()),
                DEFAULT_REGISTER_DEPARTMENT_ID,
                List.of("USER"),
                List.of(),
                copyProfile(request.rawProfile(), provider)
        );
    }

    private ThirdPartyLoginRequest fromLegacyThirdPartyRequest(AuthLoginRequest request) {
        Map<String, String> thirdPartyInfo = request.thirdPartyInfo();
        return new ThirdPartyLoginRequest(
                firstNonBlank(thirdPartyInfo.get("provider"), "UNKNOWN"),
                firstNonBlank(
                        thirdPartyInfo.get("providerUserId"),
                        thirdPartyInfo.get("subject"),
                        thirdPartyInfo.get("openId"),
                        thirdPartyInfo.get("openid")
                ),
                firstNonBlank(
                        thirdPartyInfo.get("providerUnionId"),
                        thirdPartyInfo.get("unionId"),
                        thirdPartyInfo.get("unionid")
                ),
                request.account(),
                thirdPartyInfo.get("email"),
                thirdPartyInfo.get("phone"),
                firstNonBlank(
                        thirdPartyInfo.get("userName"),
                        thirdPartyInfo.get("nickname"),
                        thirdPartyInfo.get("displayName")
                ),
                firstNonBlank(
                        thirdPartyInfo.get("avatar"),
                        thirdPartyInfo.get("avatarUrl"),
                        thirdPartyInfo.get("headImgUrl")
                ),
                thirdPartyInfo,
                parseAutoRegister(thirdPartyInfo.get("autoRegister"))
        );
    }

    private Boolean resolveFlag(Boolean requestValue, Boolean propertyValue) {
        return requestValue != null ? requestValue : propertyValue;
    }

    private String ensureUniqueAccount(String preferredAccount) {
        String normalized = preferredAccount.trim();
        String candidate = normalized;
        int suffix = 1;
        while (userAccountRepository.findByAccount(candidate).isPresent()) {
            candidate = normalized + "_" + suffix++;
        }
        return candidate;
    }

    private String deriveUserNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String generateTechnicalPassword(String source) {
        return "Auth" + source.toUpperCase(Locale.ROOT) + "9" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("EMAIL_REQUIRED", "email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException("PROVIDER_REQUIRED", "provider is required");
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    private String canonicalLoginType(String loginType) {
        if (loginType == null || loginType.isBlank()) {
            return "password";
        }
        String normalized = loginType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "password" -> "password";
            case "thirdparty", "third_party" -> "thirdParty";
            default -> loginType.trim();
        };
    }

    private String sanitizeSubject(String providerUserId) {
        return providerUserId == null ? UUID.randomUUID().toString().substring(0, 8) : providerUserId.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, String> copyProfile(Map<String, String> rawProfile, String provider) {
        LinkedHashMap<String, String> profile = new LinkedHashMap<>();
        profile.put("authSource", "THIRD_PARTY");
        profile.put("provider", provider);
        if (rawProfile != null) {
            rawProfile.forEach((key, value) -> {
                if (key != null && value != null) {
                    profile.put(key, value);
                }
            });
        }
        return Map.copyOf(profile);
    }

    private void ensureThirdPartyContactCanProvision(String email, String phone) {
        if (userAccountRepository.existsByEmail(email, null)) {
            throw new BusinessException("THIRD_PARTY_EMAIL_CONFLICT", "third-party email is already bound", HttpStatus.CONFLICT);
        }
        if (userAccountRepository.existsByPhone(phone, null)) {
            throw new BusinessException("THIRD_PARTY_PHONE_CONFLICT", "third-party phone is already bound", HttpStatus.CONFLICT);
        }
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

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private Boolean parseAutoRegister(String value) {
        if (value == null || value.isBlank()) {
            return Boolean.TRUE;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
