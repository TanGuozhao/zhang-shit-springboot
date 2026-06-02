package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.ArchitectureOverviewResponse;
import com.example.platform.user.service.UserArchitectureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/architecture")
public class UserArchitectureController {

    private final UserArchitectureService userArchitectureService;

    public UserArchitectureController(UserArchitectureService userArchitectureService) {
        this.userArchitectureService = userArchitectureService;
    }

    @GetMapping("/overview")
    public ApiResponse<ArchitectureOverviewResponse> overview() {
        return ApiResponse.ok(userArchitectureService.overview());
    }
}
