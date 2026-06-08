package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.dto.LoginRequest;
import com.example.platform.topbiz.dto.LoginResponse;
import com.example.platform.topbiz.dto.OAuthAuthorizeResponse;
import com.example.platform.topbiz.service.TopbizAuthenticationService;
import com.example.platform.topbiz.service.TopbizExternalAuthenticationService;
import com.example.platform.user.dto.EmailCodeLoginRequest;
import com.example.platform.user.dto.EmailLoginSendCodeRequest;
import com.example.platform.user.dto.EmailLoginSendCodeResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topbiz/auth")
public class TopbizAuthController {

    private final TopbizAuthenticationService topbizAuthenticationService;
    private final TopbizExternalAuthenticationService topbizExternalAuthenticationService;

    public TopbizAuthController(TopbizAuthenticationService topbizAuthenticationService,
                                TopbizExternalAuthenticationService topbizExternalAuthenticationService) {
        this.topbizAuthenticationService = topbizAuthenticationService;
        this.topbizExternalAuthenticationService = topbizExternalAuthenticationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(topbizAuthenticationService.login(request.account(), request.password()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        topbizAuthenticationService.logout();
        return ApiResponse.ok("logout success");
    }

    @PostMapping("/email/send-code")
    public ApiResponse<EmailLoginSendCodeResponse> sendEmailLoginCode(@Valid @RequestBody EmailLoginSendCodeRequest request) {
        return ApiResponse.ok(topbizExternalAuthenticationService.sendEmailLoginCode(request));
    }

    @PostMapping("/email/login")
    public ApiResponse<LoginResponse> loginByEmailCode(@Valid @RequestBody EmailCodeLoginRequest request) {
        return ApiResponse.ok(topbizExternalAuthenticationService.loginByEmailCode(request));
    }

    @GetMapping("/oauth/{provider}/authorize")
    public ApiResponse<OAuthAuthorizeResponse> authorize(@PathVariable String provider) {
        return ApiResponse.ok(topbizExternalAuthenticationService.buildAuthorizeResponse(provider));
    }

    @GetMapping("/oauth/{provider}/callback")
    public ApiResponse<LoginResponse> callback(@PathVariable String provider,
                                               @RequestParam String code,
                                               @RequestParam String state) {
        return ApiResponse.ok(topbizExternalAuthenticationService.completeOAuthLogin(provider, code, state));
    }

    @GetMapping("/session")
    public ApiResponse<LoginResponse> currentSession() {
        return ApiResponse.ok(topbizAuthenticationService.currentSession());
    }
}
