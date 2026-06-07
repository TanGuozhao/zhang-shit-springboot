package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.dto.DepartmentTransferOrchestrationRequest;
import com.example.platform.topbiz.dto.MessageAuditOrchestrationRequest;
import com.example.platform.topbiz.dto.OrchestrationExecutionResponse;
import com.example.platform.topbiz.dto.UserProvisioningOrchestrationRequest;
import com.example.platform.topbiz.security.TopbizPermissions;
import com.example.platform.topbiz.service.TopbizOrchestrationService;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topbiz/orchestrations")
public class TopbizOrchestrationController {

    private final TopbizOrchestrationService topbizOrchestrationService;

    public TopbizOrchestrationController(TopbizOrchestrationService topbizOrchestrationService) {
        this.topbizOrchestrationService = topbizOrchestrationService;
    }

    @PostMapping("/user-provisioning")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_ORCHESTRATION_WRITE)
    public ApiResponse<OrchestrationExecutionResponse> executeUserProvisioning(
            @Valid @RequestBody UserProvisioningOrchestrationRequest request) {
        return ApiResponse.ok(topbizOrchestrationService.executeUserProvisioning(request));
    }

    @PostMapping("/department-transfer")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_ORCHESTRATION_WRITE)
    public ApiResponse<OrchestrationExecutionResponse> executeDepartmentTransfer(
            @Valid @RequestBody DepartmentTransferOrchestrationRequest request) {
        return ApiResponse.ok(topbizOrchestrationService.executeDepartmentTransfer(request));
    }

    @PostMapping("/message-audit")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_ORCHESTRATION_WRITE)
    public ApiResponse<OrchestrationExecutionResponse> executeMessageAudit(
            @Valid @RequestBody MessageAuditOrchestrationRequest request) {
        return ApiResponse.ok(topbizOrchestrationService.executeMessageAudit(request));
    }

    @GetMapping
    @RequiresPermissions(TopbizPermissions.TOPBIZ_PLATFORM_READ)
    public ApiResponse<List<OrchestrationExecutionResponse>> listExecutions(
            @RequestParam(value = "orchestrationType", required = false) String orchestrationType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResponse.ok(topbizOrchestrationService.listExecutions(orchestrationType, status, limit));
    }

    @GetMapping("/{orchestrationId}")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_PLATFORM_READ)
    public ApiResponse<OrchestrationExecutionResponse> getExecution(@PathVariable String orchestrationId) {
        return ApiResponse.ok(topbizOrchestrationService.getExecution(orchestrationId));
    }
}
