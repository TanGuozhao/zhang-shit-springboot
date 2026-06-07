package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.dto.PlatformArchitectureResponse;
import com.example.platform.topbiz.dto.PlatformOverviewResponse;
import com.example.platform.topbiz.dto.PlatformRuntimeResponse;
import com.example.platform.topbiz.security.TopbizPermissions;
import com.example.platform.topbiz.service.PlatformOrchestrationService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topbiz/platform")
public class PlatformController {

    private final PlatformOrchestrationService platformOrchestrationService;

    public PlatformController(PlatformOrchestrationService platformOrchestrationService) {
        this.platformOrchestrationService = platformOrchestrationService;
    }

    @GetMapping("/overview")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_PLATFORM_READ)
    public ApiResponse<PlatformOverviewResponse> overview() {
        return ApiResponse.ok(platformOrchestrationService.overview());
    }

    @GetMapping("/architecture")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_ARCHITECTURE_READ)
    public ApiResponse<PlatformArchitectureResponse> architecture() {
        return ApiResponse.ok(platformOrchestrationService.architecture());
    }

    @GetMapping("/runtime")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<PlatformRuntimeResponse> runtime() {
        return ApiResponse.ok(platformOrchestrationService.runtime());
    }
}
