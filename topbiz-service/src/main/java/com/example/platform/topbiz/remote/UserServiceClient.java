package com.example.platform.topbiz.remote;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.config.TopbizFeignConfig;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginRequest;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginResponse;
import com.example.platform.topbiz.remote.dto.RemoteDepartmentResponse;
import com.example.platform.topbiz.remote.dto.RemotePermissionListResponse;
import com.example.platform.topbiz.remote.dto.RemoteRoleListResponse;
import com.example.platform.topbiz.remote.dto.RemoteUserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "userServiceClient", url = "${topbiz.remote.user-service.base-url}", configuration = TopbizFeignConfig.class)
public interface UserServiceClient {

    @PostMapping("/api/users/auth/login")
    ApiResponse<RemoteAuthLoginResponse> login(@RequestBody RemoteAuthLoginRequest request);

    @GetMapping("/api/users/me")
    ApiResponse<RemoteUserProfileResponse> me();

    @GetMapping("/api/users/{userId}")
    ApiResponse<RemoteUserProfileResponse> getUser(@PathVariable Long userId);

    @GetMapping("/api/users/{userId}/permissions")
    ApiResponse<RemotePermissionListResponse> getPermissions(@PathVariable Long userId);

    @GetMapping("/api/users/{userId}/roles")
    ApiResponse<RemoteRoleListResponse> getRoles(@PathVariable Long userId);

    @GetMapping("/api/users/departments/{deptId}")
    ApiResponse<RemoteDepartmentResponse> getDepartment(@PathVariable Long deptId);
}
