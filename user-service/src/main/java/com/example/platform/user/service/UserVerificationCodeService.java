package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.config.UserServiceProperties;
import com.example.platform.user.domain.UserAccount;
import com.example.platform.user.domain.VerificationCode;
import com.example.platform.user.repository.UserAccountRepository;
import com.example.platform.user.repository.VerificationCodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class UserVerificationCodeService {

    public static final String SCENE_REGISTER = "REGISTER";
    public static final String SCENE_FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String SCENE_UNFREEZE = "UNFREEZE";
    public static final String SCENE_EMAIL_LOGIN = "EMAIL_LOGIN";

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserServiceProperties userServiceProperties;
    private final UserVerificationCodeSender userVerificationCodeSender;

    public UserVerificationCodeService(VerificationCodeRepository verificationCodeRepository,
                                       UserAccountRepository userAccountRepository,
                                       UserServiceProperties userServiceProperties,
                                       UserVerificationCodeSender userVerificationCodeSender) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userAccountRepository = userAccountRepository;
        this.userServiceProperties = userServiceProperties;
        this.userVerificationCodeSender = userVerificationCodeSender;
    }

    public VerificationCode send(String account, String contact, String scene) {
        String normalizedScene = normalizeScene(scene);
        validateScene(normalizedScene);
        validateSendAllowed(account, contact, normalizedScene);
        enforceCooldown(account, normalizedScene);
        VerificationCode verificationCode = verificationCodeRepository.save(
                account,
                contact,
                normalizedScene,
                Duration.ofMinutes(userServiceProperties.verifyCodeTtlMinutes())
        );
        userVerificationCodeSender.send(verificationCode);
        return verificationCode;
    }

    public void verifyOrThrow(String account, String contact, String scene, String code) {
        String normalizedScene = normalizeScene(scene);
        if (!verificationCodeRepository.verify(account, contact, normalizedScene, code)) {
            throw new BusinessException("INVALID_VERIFY_CODE", "verify code is invalid");
        }
    }

    public void consume(String account, String scene) {
        verificationCodeRepository.remove(account, normalizeScene(scene));
    }

    public String normalizeScene(String scene) {
        return scene == null ? "" : scene.trim().toUpperCase(Locale.ROOT);
    }

    private void validateScene(String scene) {
        if (!SCENE_REGISTER.equals(scene)
                && !SCENE_FORGOT_PASSWORD.equals(scene)
                && !SCENE_UNFREEZE.equals(scene)
                && !SCENE_EMAIL_LOGIN.equals(scene)) {
            throw new BusinessException("INVALID_VERIFY_SCENE", "scene must be REGISTER, FORGOT_PASSWORD, UNFREEZE or EMAIL_LOGIN");
        }
    }

    private void validateSendAllowed(String account, String contact, String scene) {
        if (SCENE_REGISTER.equals(scene)) {
            if (userAccountRepository.findByAccount(account).isPresent()) {
                throw new BusinessException("ACCOUNT_ALREADY_EXISTS", "account already exists");
            }
            return;
        }
        if (SCENE_EMAIL_LOGIN.equals(scene)) {
            return;
        }

        UserAccount user = userAccountRepository.findByAccountAndContact(account, contact)
                .orElseThrow(() -> new BusinessException("ACCOUNT_CONTACT_MISMATCH", "account and contact do not match"));
        if (SCENE_UNFREEZE.equals(scene) && !"FROZEN".equals(user.status())) {
            throw new BusinessException("INVALID_ACCOUNT_STATUS", "account is not frozen");
        }
    }

    private void enforceCooldown(String account, String scene) {
        verificationCodeRepository.find(account, scene).ifPresent(existing -> {
            Instant availableAt = existing.issuedAt().plusSeconds(userServiceProperties.verifyCodeCooldownSeconds());
            if (availableAt.isAfter(Instant.now())) {
                throw new BusinessException(
                        "VERIFY_CODE_COOLDOWN",
                        "verify code was sent too recently",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
        });
    }
}
