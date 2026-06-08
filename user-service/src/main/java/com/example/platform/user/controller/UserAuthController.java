package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
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
import com.example.platform.user.service.UserAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/verify-codes")
    public ApiResponse<VerifyCodeSendResponse> sendVerifyCode(@Valid @RequestBody VerifyCodeSendRequest request) {
        return ApiResponse.ok(userAuthService.sendVerifyCode(request));
    }

    @PostMapping("/register")
    public ApiResponse<UserRegistrationResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ApiResponse.ok(userAuthService.register(request));
    }

    @PostMapping("/email/send-code")
    public ApiResponse<EmailLoginSendCodeResponse> sendEmailLoginCode(@Valid @RequestBody EmailLoginSendCodeRequest request) {
        return ApiResponse.ok(userAuthService.sendEmailLoginCode(request));
    }

    @PostMapping("/email/login")
    public ApiResponse<AuthLoginResponse> loginByEmailCode(@Valid @RequestBody EmailCodeLoginRequest request) {
        return ApiResponse.ok(userAuthService.loginByEmailCode(request));
    }

    @PostMapping("/third-party/login")
    public ApiResponse<AuthLoginResponse> loginByThirdParty(@Valid @RequestBody ThirdPartyLoginRequest request) {
        return ApiResponse.ok(userAuthService.loginByThirdParty(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.ok(userAuthService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "X-Session-Key", required = false) String sessionKey) {
        userAuthService.logout(sessionKey);
        return ApiResponse.ok("logout success");
    }
}
