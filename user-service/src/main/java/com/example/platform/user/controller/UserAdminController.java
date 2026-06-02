package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserAuthorizationUpdateRequest;
import com.example.platform.user.dto.UserCreateRequest;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import com.example.platform.user.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/admin")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @PostMapping
    public ApiResponse<UserProfileResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userAdminService.createUser(request));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserProfileResponse> updateStatus(@PathVariable Long userId,
                                                         @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.ok(userAdminService.updateStatus(userId, request));
    }

    @PatchMapping("/{userId}/authorization")
    public ApiResponse<RoleListResponse> updateAuthorization(@PathVariable Long userId,
                                                             @Valid @RequestBody UserAuthorizationUpdateRequest request) {
        return ApiResponse.ok(userAdminService.updateAuthorization(userId, request));
    }

    @PostMapping("/{userId}/authorization/permissions:refresh")
    public ApiResponse<PermissionListResponse> refreshPermissions(@PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.getUpdatedPermissions(userId));
    }
}
