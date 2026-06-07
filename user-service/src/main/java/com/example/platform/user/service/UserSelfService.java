package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.config.UserServiceProperties;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.dto.ForgotPasswordResetRequest;
import com.example.platform.user.dto.ForgotPasswordSendCodeRequest;
import com.example.platform.user.dto.ForgotPasswordSendCodeResponse;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.PasswordChangeRequest;
import com.example.platform.user.dto.UserCancelRequest;
import com.example.platform.user.dto.UserProfileModificationRecordResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserProfileUpdateRequest;
import com.example.platform.user.dto.UserStatusResponse;
import com.example.platform.user.dto.UserUnfreezeRequest;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.UserProfileModificationRepository;
import com.example.platform.user.repository.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserSelfService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileModificationRepository userProfileModificationRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserPasswordPolicyService userPasswordPolicyService;
    private final UserAccessSupport userAccessSupport;
    private final UserViewAssembler userViewAssembler;
    private final UserVerificationCodeService userVerificationCodeService;
    private final UserServiceProperties userServiceProperties;

    public UserSelfService(UserAccountRepository userAccountRepository,
                           UserProfileModificationRepository userProfileModificationRepository,
                           UserSessionRepository userSessionRepository,
                           UserPasswordPolicyService userPasswordPolicyService,
                           UserAccessSupport userAccessSupport,
                           UserViewAssembler userViewAssembler,
                           UserVerificationCodeService userVerificationCodeService,
                           UserServiceProperties userServiceProperties) {
        this.userAccountRepository = userAccountRepository;
        this.userProfileModificationRepository = userProfileModificationRepository;
        this.userSessionRepository = userSessionRepository;
        this.userPasswordPolicyService = userPasswordPolicyService;
        this.userAccessSupport = userAccessSupport;
        this.userViewAssembler = userViewAssembler;
        this.userVerificationCodeService = userVerificationCodeService;
        this.userServiceProperties = userServiceProperties;
    }

    public UserProfileResponse updateProfile(Long userId, String sessionKey, UserProfileUpdateRequest request) {
        UserAccount existing = userAccessSupport.requireCurrentUser(userId, sessionKey);
        if (!userPasswordPolicyService.matches(request.password(), existing.password())) {
            throw userPasswordPolicyService.invalidCredentials();
        }

        String nextUserName = request.userName() == null || request.userName().isBlank()
                ? existing.userName()
                : request.userName().trim();
        String nextEmail = existing.email();
        String nextPhone = existing.phone();
        if (request.contact() != null && !request.contact().isBlank()) {
            if (request.contact().contains("@")) {
                nextEmail = request.contact().trim();
            } else if (request.contact().matches("\\d{11}")) {
                nextPhone = request.contact().trim();
            } else {
                throw new BusinessException("INVALID_CONTACT", "contact must be email or 11-digit phone");
            }
        }
        ensureUniqueContact(nextEmail, nextPhone, existing.userId());

        String nextAvatar = request.avatar() == null || request.avatar().isBlank()
                ? existing.avatar()
                : request.avatar().trim();
        Map<String, String> nextExtFields = mergeExtFields(existing.extFields(), request.extFields());

        UserAccount updated = userAccountRepository.updateProfile(
                existing.userId(),
                nextUserName,
                nextEmail,
                nextPhone,
                nextAvatar,
                nextExtFields
        ).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        appendModificationRecords(existing, updated);
        return userViewAssembler.toProfile(updated);
    }

    public PagedResult<UserProfileModificationRecordResponse> listModificationRecords(Long userId,
                                                                                      String sessionKey,
                                                                                      Integer pageNum,
                                                                                      Integer pageSize) {
        UserAccount currentUser = userAccessSupport.requireCurrentUser(userId, sessionKey);
        List<UserProfileModificationRecordResponse> records = userProfileModificationRepository.findByUserId(currentUser.userId())
                .stream()
                .map(userViewAssembler::toModificationRecord)
                .toList();
        return page(records, pageNum, pageSize);
    }

    public void changePassword(Long userId, String sessionKey, PasswordChangeRequest request) {
        UserAccount currentUser = userAccessSupport.requireCurrentUser(userId, sessionKey);
        if (!userPasswordPolicyService.matches(request.oldPassword(), currentUser.password())) {
            throw userPasswordPolicyService.invalidCredentials();
        }
        userPasswordPolicyService.ensureConfirmed(request.newPassword(), request.confirmPassword());
        userPasswordPolicyService.validateStrength(request.newPassword());

        String encodedPassword = userPasswordPolicyService.encode(request.newPassword());
        if (userPasswordPolicyService.matches(request.newPassword(), currentUser.password())) {
            throw new BusinessException("PASSWORD_REUSE_FORBIDDEN", "new password must be different from old password");
        }
        if (userAccountRepository.passwordUsedRecently(
                currentUser.userId(),
                request.newPassword(),
                userServiceProperties.passwordHistoryLimit())) {
            throw new BusinessException("PASSWORD_REUSE_FORBIDDEN", "password was used recently");
        }

        userAccountRepository.updatePassword(currentUser.userId(), encodedPassword)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        userSessionRepository.removeByUserId(currentUser.userId());
    }

    public ForgotPasswordSendCodeResponse sendForgotPasswordCode(ForgotPasswordSendCodeRequest request) {
        var code = userVerificationCodeService.send(
                request.account(),
                request.contact(),
                UserVerificationCodeService.SCENE_FORGOT_PASSWORD
        );
        return new ForgotPasswordSendCodeResponse(code.account(), code.contact(), code.expiresAt());
    }

    public void resetForgottenPassword(ForgotPasswordResetRequest request) {
        userPasswordPolicyService.ensureConfirmed(request.newPassword(), request.confirmPassword());
        userPasswordPolicyService.validateStrength(request.newPassword());
        userVerificationCodeService.verifyOrThrow(
                request.account(),
                request.contact(),
                UserVerificationCodeService.SCENE_FORGOT_PASSWORD,
                request.verifyCode()
        );

        UserAccount user = userAccountRepository.findByAccountAndContact(request.account(), request.contact())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        String encodedPassword = userPasswordPolicyService.encode(request.newPassword());
        if (userAccountRepository.passwordUsedRecently(
                user.userId(),
                request.newPassword(),
                userServiceProperties.passwordHistoryLimit())) {
            throw new BusinessException("PASSWORD_REUSE_FORBIDDEN", "password was used recently");
        }

        userAccountRepository.updatePassword(user.userId(), encodedPassword)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        userSessionRepository.removeByUserId(user.userId());
        userVerificationCodeService.consume(request.account(), UserVerificationCodeService.SCENE_FORGOT_PASSWORD);
    }

    public UserStatusResponse getCurrentStatus(Long userId, String sessionKey) {
        return userViewAssembler.toStatus(userAccessSupport.requireCurrentUser(userId, sessionKey));
    }

    public UserStatusResponse applyUnfreeze(Long userId, String sessionKey, UserUnfreezeRequest request) {
        UserAccount targetUser;
        String account;
        String contact;
        if (sessionKey != null || userId != null) {
            targetUser = userAccessSupport.requireCurrentUser(userId, sessionKey);
            account = targetUser.account();
            contact = resolveContact(targetUser);
        } else {
            if (request.account() == null || request.account().isBlank()
                    || request.contact() == null || request.contact().isBlank()) {
                throw new BusinessException("ACCOUNT_CONTACT_REQUIRED", "account and contact are required when not logged in");
            }
            account = request.account();
            contact = request.contact();
            targetUser = userAccountRepository.findByAccountAndContact(account, contact)
                    .orElseThrow(() -> new BusinessException("ACCOUNT_CONTACT_MISMATCH", "account and contact do not match"));
        }

        if (!"FROZEN".equals(targetUser.status())) {
            throw new BusinessException("INVALID_ACCOUNT_STATUS", "account is not frozen");
        }
        userVerificationCodeService.verifyOrThrow(
                account,
                contact,
                UserVerificationCodeService.SCENE_UNFREEZE,
                request.verifyCode()
        );

        UserAccount updated = userAccountRepository.updateStatus(targetUser.userId(), "ENABLED")
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        userVerificationCodeService.consume(account, UserVerificationCodeService.SCENE_UNFREEZE);
        return userViewAssembler.toStatus(updated);
    }

    public UserStatusResponse applyCancel(Long userId, String sessionKey, UserCancelRequest request) {
        UserAccount currentUser = userAccessSupport.requireCurrentUser(userId, sessionKey);
        if (!userPasswordPolicyService.matches(request.password(), currentUser.password())) {
            throw userPasswordPolicyService.invalidCredentials();
        }

        UserAccount updated = userAccountRepository.updateStatus(currentUser.userId(), "CANCEL_PENDING")
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
        userSessionRepository.removeByUserId(currentUser.userId());
        return userViewAssembler.toStatus(updated);
    }

    private void ensureUniqueContact(String email, String phone, Long excludedUserId) {
        if (userAccountRepository.existsByEmail(email, excludedUserId)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "email already exists");
        }
        if (userAccountRepository.existsByPhone(phone, excludedUserId)) {
            throw new BusinessException("PHONE_ALREADY_EXISTS", "phone already exists");
        }
    }

    private String resolveContact(UserAccount user) {
        if (user.email() != null && !user.email().isBlank()) {
            return user.email();
        }
        return user.phone();
    }

    private void appendModificationRecords(UserAccount existing, UserAccount updated) {
        appendIfChanged(existing.userId(), "userName", existing.userName(), updated.userName());
        appendIfChanged(existing.userId(), "email", existing.email(), updated.email());
        appendIfChanged(existing.userId(), "phone", existing.phone(), updated.phone());
        appendIfChanged(existing.userId(), "avatar", existing.avatar(), updated.avatar());
        if (!existing.extFields().equals(updated.extFields())) {
            userProfileModificationRepository.append(
                    existing.userId(),
                    "extFields",
                    existing.extFields().toString(),
                    updated.extFields().toString()
            );
        }
    }

    private void appendIfChanged(Long userId, String fieldName, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            userProfileModificationRepository.append(userId, fieldName, oldValue, newValue);
        }
    }

    private Map<String, String> mergeExtFields(Map<String, String> existing, Map<String, String> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return existing;
        }
        Map<String, String> merged = new LinkedHashMap<>(existing);
        merged.putAll(incoming);
        return Map.copyOf(merged);
    }

    private <T> PagedResult<T> page(List<T> items, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, items.size());
        int toIndex = Math.min(fromIndex + safePageSize, items.size());
        return new PagedResult<>(items.size(), items.subList(fromIndex, toIndex));
    }
}
