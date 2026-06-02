package com.example.platform.log.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.log.dto.ArchitectureOverviewResponse;
import com.example.platform.log.service.LogArchitectureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/architecture")
public class LogArchitectureController {

    private final LogArchitectureService logArchitectureService;

    public LogArchitectureController(LogArchitectureService logArchitectureService) {
        this.logArchitectureService = logArchitectureService;
    }

    @GetMapping("/overview")
    public ApiResponse<ArchitectureOverviewResponse> overview() {
        return ApiResponse.ok(logArchitectureService.overview());
    }
}
