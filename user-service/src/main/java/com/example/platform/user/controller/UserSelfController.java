package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
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
import com.example.platform.user.service.UserSelfService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserSelfController {

    private final UserSelfService userSelfService;

    public UserSelfController(UserSelfService userSelfService) {
        this.userSelfService = userSelfService;
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.ok(userSelfService.updateProfile(userId, sessionKey, request));
    }

    @GetMapping("/me/modify-records")
    public ApiResponse<PagedResult<UserProfileModificationRecordResponse>> listModificationRecords(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(userSelfService.listModificationRecords(userId, sessionKey, pageNum, pageSize));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody PasswordChangeRequest request) {
        userSelfService.changePassword(userId, sessionKey, request);
        return ApiResponse.ok("password updated");
    }

    @PostMapping("/password/forgot/send-code")
    public ApiResponse<ForgotPasswordSendCodeResponse> sendForgotPasswordCode(
            @Valid @RequestBody ForgotPasswordSendCodeRequest request) {
        return ApiResponse.ok(userSelfService.sendForgotPasswordCode(request));
    }

    @PostMapping("/password/forgot/reset")
    public ApiResponse<Void> resetForgottenPassword(@Valid @RequestBody ForgotPasswordResetRequest request) {
        userSelfService.resetForgottenPassword(request);
        return ApiResponse.ok("password reset");
    }

    @GetMapping("/me/status")
    public ApiResponse<UserStatusResponse> getCurrentStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey) {
        return ApiResponse.ok(userSelfService.getCurrentStatus(userId, sessionKey));
    }

    @PostMapping("/me/status/unfreeze")
    public ApiResponse<UserStatusResponse> applyUnfreeze(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody UserUnfreezeRequest request) {
        return ApiResponse.ok(userSelfService.applyUnfreeze(userId, sessionKey, request));
    }

    @PostMapping("/me/status/cancel")
    public ApiResponse<UserStatusResponse> applyCancel(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody UserCancelRequest request) {
        return ApiResponse.ok(userSelfService.applyCancel(userId, sessionKey, request));
    }
}
