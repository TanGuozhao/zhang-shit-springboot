package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.service.UserQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserQueryController {

    private final UserQueryService userQueryService;

    public UserQueryController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> currentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey) {
        return ApiResponse.ok(userQueryService.getCurrentUser(userId, sessionKey));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(userQueryService.getUser(userId));
    }

    @GetMapping("/{userId}/permissions")
    public ApiResponse<PermissionListResponse> getPermissions(@PathVariable Long userId) {
        return ApiResponse.ok(userQueryService.getPermissions(userId));
    }

    @GetMapping("/{userId}/roles")
    public ApiResponse<RoleListResponse> getRoles(@PathVariable Long userId) {
        return ApiResponse.ok(userQueryService.getRoles(userId));
    }
}
