package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.dto.LoginRequest;
import com.example.platform.topbiz.dto.LoginResponse;
import com.example.platform.topbiz.service.TopbizAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topbiz/auth")
public class TopbizAuthController {

    private final TopbizAuthenticationService topbizAuthenticationService;

    public TopbizAuthController(TopbizAuthenticationService topbizAuthenticationService) {
        this.topbizAuthenticationService = topbizAuthenticationService;
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
}
