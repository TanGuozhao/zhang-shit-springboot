package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.security.TopbizPermissions;
import com.example.platform.topbiz.service.TopbizUserGatewayService;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.dto.ForgotPasswordResetRequest;
import com.example.platform.user.dto.ForgotPasswordSendCodeRequest;
import com.example.platform.user.dto.ForgotPasswordSendCodeResponse;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.dto.PasswordChangeRequest;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserCancelRequest;
import com.example.platform.user.dto.UserProfileModificationRecordResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserProfileUpdateRequest;
import com.example.platform.user.dto.UserRegistrationRequest;
import com.example.platform.user.dto.UserRegistrationResponse;
import com.example.platform.user.dto.UserStatusResponse;
import com.example.platform.user.dto.UserUnfreezeRequest;
import com.example.platform.user.dto.VerifyCodeSendRequest;
import com.example.platform.user.dto.VerifyCodeSendResponse;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topbiz/users")
public class TopbizUserController {

    private final TopbizUserGatewayService topbizUserGatewayService;

    public TopbizUserController(TopbizUserGatewayService topbizUserGatewayService) {
        this.topbizUserGatewayService = topbizUserGatewayService;
    }

    @PostMapping("/auth/verify-codes")
    public ApiResponse<VerifyCodeSendResponse> sendVerifyCode(@Valid @RequestBody VerifyCodeSendRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.sendVerifyCode(request));
    }

    @PostMapping("/auth/register")
    public ApiResponse<UserRegistrationResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.register(request));
    }

    @GetMapping("/me")
    @RequiresPermissions(TopbizPermissions.USER_SELF_READ)
    public ApiResponse<UserProfileResponse> currentUser() {
        return ApiResponse.ok(topbizUserGatewayService.currentUser());
    }

    @PutMapping("/me")
    @RequiresPermissions(TopbizPermissions.USER_SELF_WRITE)
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.updateProfile(request));
    }

    @GetMapping("/me/modify-records")
    @RequiresPermissions(TopbizPermissions.USER_SELF_READ)
    public ApiResponse<PagedResult<UserProfileModificationRecordResponse>> listModificationRecords(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(topbizUserGatewayService.listModificationRecords(pageNum, pageSize));
    }

    @PutMapping("/me/password")
    @RequiresPermissions(TopbizPermissions.USER_SELF_WRITE)
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        topbizUserGatewayService.changePassword(request);
        return ApiResponse.ok("password updated");
    }

    @PostMapping("/password/forgot/send-code")
    public ApiResponse<ForgotPasswordSendCodeResponse> sendForgotPasswordCode(
            @Valid @RequestBody ForgotPasswordSendCodeRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.sendForgotPasswordCode(request));
    }

    @PostMapping("/password/forgot/reset")
    public ApiResponse<Void> resetForgottenPassword(@Valid @RequestBody ForgotPasswordResetRequest request) {
        topbizUserGatewayService.resetForgottenPassword(request);
        return ApiResponse.ok("password reset");
    }

    @GetMapping("/me/status")
    @RequiresPermissions(TopbizPermissions.USER_SELF_READ)
    public ApiResponse<UserStatusResponse> getCurrentStatus() {
        return ApiResponse.ok(topbizUserGatewayService.currentStatus());
    }

    @PostMapping("/me/status/unfreeze")
    public ApiResponse<UserStatusResponse> applyUnfreeze(@Valid @RequestBody UserUnfreezeRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.applyUnfreeze(request));
    }

    @PostMapping("/me/status/cancel")
    @RequiresPermissions(TopbizPermissions.USER_SELF_WRITE)
    public ApiResponse<UserStatusResponse> applyCancel(@Valid @RequestBody UserCancelRequest request) {
        return ApiResponse.ok(topbizUserGatewayService.applyCancel(request));
    }

    @GetMapping("/{userId}")
    @RequiresPermissions(TopbizPermissions.USER_READ)
    public ApiResponse<UserProfileResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(topbizUserGatewayService.getUser(userId));
    }

    @GetMapping("/{userId}/permissions")
    @RequiresPermissions(TopbizPermissions.USER_READ)
    public ApiResponse<PermissionListResponse> getPermissions(@PathVariable Long userId) {
        return ApiResponse.ok(topbizUserGatewayService.getPermissions(userId));
    }

    @GetMapping("/{userId}/roles")
    @RequiresPermissions(TopbizPermissions.USER_READ)
    public ApiResponse<RoleListResponse> getRoles(@PathVariable Long userId) {
        return ApiResponse.ok(topbizUserGatewayService.getRoles(userId));
    }

    @GetMapping("/departments/{deptId}")
    @RequiresPermissions(TopbizPermissions.DEPARTMENT_READ)
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable Long deptId) {
        return ApiResponse.ok(topbizUserGatewayService.getDepartment(deptId));
    }
}
