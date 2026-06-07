package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.AdminPasswordResetRequest;
import com.example.platform.user.dto.AdminUserSummaryResponse;
import com.example.platform.user.dto.AdminUserUpdateRequest;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserAuthorizationUpdateRequest;
import com.example.platform.user.dto.UserCreateRequest;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import com.example.platform.user.service.UserAdminService;
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
@RequestMapping("/api/users/admin")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<PagedResult<AdminUserSummaryResponse>> listUsers(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(userAdminService.listUsers(
                operatorUserId,
                sessionKey,
                account,
                userName,
                status,
                pageNum,
                pageSize
        ));
    }

    @PostMapping
    public ApiResponse<UserProfileResponse> createUser(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userAdminService.createUser(operatorUserId, sessionKey, request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserProfileResponse> updateUser(
                                                       @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
                                                       @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
                                                       @PathVariable Long userId,
                                                       @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.ok(userAdminService.updateUser(operatorUserId, sessionKey, userId, request));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserProfileResponse> updateStatus(
                                                         @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
                                                         @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
                                                         @PathVariable Long userId,
                                                         @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.ok(userAdminService.updateStatus(operatorUserId, sessionKey, userId, request));
    }

    @PatchMapping("/{userId}/authorization")
    public ApiResponse<RoleListResponse> updateAuthorization(
                                                             @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
                                                             @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
                                                             @PathVariable Long userId,
                                                             @Valid @RequestBody UserAuthorizationUpdateRequest request) {
        return ApiResponse.ok(userAdminService.updateAuthorization(operatorUserId, sessionKey, userId, request));
    }

    @PostMapping("/{userId}/authorization/permissions:refresh")
    public ApiResponse<PermissionListResponse> refreshPermissions(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.getUpdatedPermissions(operatorUserId, sessionKey, userId));
    }

    @PostMapping("/{userId}/password:reset")
    public ApiResponse<Void> resetPassword(
                                           @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
                                           @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
                                           @PathVariable Long userId,
                                           @Valid @RequestBody AdminPasswordResetRequest request) {
        userAdminService.resetPassword(operatorUserId, sessionKey, userId, request);
        return ApiResponse.ok("password reset");
    }
}
