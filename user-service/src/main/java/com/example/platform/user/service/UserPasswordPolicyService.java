package com.example.platform.user.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.user.config.UserServiceProperties;
import com.example.platform.user.support.PasswordHashCodec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UserPasswordPolicyService {

    private final UserServiceProperties userServiceProperties;

    public UserPasswordPolicyService(UserServiceProperties userServiceProperties) {
        this.userServiceProperties = userServiceProperties;
    }

    public String encode(String rawPassword) {
        return PasswordHashCodec.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return PasswordHashCodec.matches(rawPassword, encodedPassword);
    }

    public void validateStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException("PASSWORD_REQUIRED", "password is required");
        }
        if (rawPassword.length() < userServiceProperties.passwordMinLength()) {
            throw new BusinessException(
                    "PASSWORD_TOO_WEAK",
                    "password length must be at least " + userServiceProperties.passwordMinLength()
            );
        }
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(
                    "PASSWORD_TOO_WEAK",
                    "password must contain both letters and digits"
            );
        }
    }

    public void ensureConfirmed(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("PASSWORD_CONFIRM_MISMATCH", "confirm password does not match");
        }
    }

    public BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "invalid credentials", HttpStatus.UNAUTHORIZED);
    }
}
