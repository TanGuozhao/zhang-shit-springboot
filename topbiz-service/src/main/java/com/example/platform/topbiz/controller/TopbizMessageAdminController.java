package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.CarrierAccountRequest;
import com.example.platform.message.dto.CarrierAccountResponse;
import com.example.platform.message.dto.ChannelConfigRequest;
import com.example.platform.message.dto.ChannelResponse;
import com.example.platform.message.dto.ChannelSenderUpdateRequest;
import com.example.platform.message.dto.DispatchTaskListResponse;
import com.example.platform.message.dto.DispatchTaskRequest;
import com.example.platform.message.dto.DispatchTaskResponse;
import com.example.platform.message.dto.DispatchTaskTimeUpdateRequest;
import com.example.platform.message.dto.MessageStatisticsResponse;
import com.example.platform.message.dto.RuntimeOverviewResponse;
import com.example.platform.message.dto.SchedulePolicyRequest;
import com.example.platform.message.dto.SchedulePolicyResponse;
import com.example.platform.message.dto.TaskExecutionResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplateStatusUpdateRequest;
import com.example.platform.message.dto.TemplateUpsertRequest;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.dto.VariableRequiredUpdateRequest;
import com.example.platform.message.dto.VariableTypeUpdateRequest;
import com.example.platform.message.dto.VariableUpsertRequest;
import com.example.platform.topbiz.security.TopbizPermissions;
import com.example.platform.topbiz.service.TopbizMessageGatewayService;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/topbiz/messages")
public class TopbizMessageAdminController {

    private final TopbizMessageGatewayService topbizMessageGatewayService;

    public TopbizMessageAdminController(TopbizMessageGatewayService topbizMessageGatewayService) {
        this.topbizMessageGatewayService = topbizMessageGatewayService;
    }

    @PostMapping("/admin/templates")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<TemplateDetailResponse> upsertTemplate(@RequestBody TemplateUpsertRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.upsertTemplate(request));
    }

    @PutMapping("/admin/templates/{templateCode}/status")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<TemplateDetailResponse> updateTemplateStatus(
            @PathVariable String templateCode,
            @RequestBody TemplateStatusUpdateRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.updateTemplateStatus(templateCode, request));
    }

    @PostMapping("/admin/variables")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<VariableDefinitionResponse> upsertVariable(@RequestBody VariableUpsertRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.upsertVariable(request));
    }

    @PutMapping("/admin/variables/{variableCode}/type")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<VariableDefinitionResponse> updateVariableType(
            @PathVariable String variableCode,
            @RequestBody VariableTypeUpdateRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.updateVariableType(variableCode, request));
    }

    @PutMapping("/admin/variables/{variableCode}/required")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<VariableDefinitionResponse> updateVariableRequired(
            @PathVariable String variableCode,
            @RequestBody VariableRequiredUpdateRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.updateVariableRequired(variableCode, request));
    }

    @PostMapping("/admin/carrier/accounts")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<CarrierAccountResponse> createCarrierAccount(@Valid @RequestBody CarrierAccountRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.createCarrierAccount(request));
    }

    @PostMapping("/admin/channels/config")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<ChannelResponse> configureChannel(@Valid @RequestBody ChannelConfigRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.configureChannel(request));
    }

    @PutMapping("/admin/channels/{channelCode}/sender")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<ChannelResponse> updateChannelSender(@PathVariable String channelCode,
                                                            @Valid @RequestBody ChannelSenderUpdateRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.updateChannelSender(channelCode, request));
    }

    @PostMapping("/admin/schedule/policies")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<SchedulePolicyResponse> createSchedulePolicy(@Valid @RequestBody SchedulePolicyRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.createSchedulePolicy(request));
    }

    @PostMapping("/admin/dispatch/tasks")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<DispatchTaskResponse> createDispatchTask(@RequestBody DispatchTaskRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.createDispatchTask(request));
    }

    @GetMapping("/admin/dispatch/tasks")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<DispatchTaskListResponse> listDispatchTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.ok(topbizMessageGatewayService.listDispatchTasks(status, startTime, endTime));
    }

    @PutMapping("/admin/dispatch/tasks/{taskId}/time")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<DispatchTaskResponse> updateDispatchTaskTime(
            @PathVariable String taskId,
            @RequestBody DispatchTaskTimeUpdateRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.updateDispatchTaskTime(taskId, request));
    }

    @PutMapping("/admin/dispatch/tasks/{taskId}/cancel")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<DispatchTaskResponse> cancelDispatchTask(@PathVariable String taskId) {
        return ApiResponse.ok(topbizMessageGatewayService.cancelDispatchTask(taskId));
    }

    @PostMapping("/admin/dispatch/tasks/{taskId}/trigger")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<DispatchTaskResponse> triggerDispatchTask(@PathVariable String taskId) {
        return ApiResponse.ok(topbizMessageGatewayService.triggerDispatchTask(taskId));
    }

    @PostMapping("/admin/retries/run")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<TaskExecutionResponse> runRetries() {
        return ApiResponse.ok(topbizMessageGatewayService.runRetries());
    }

    @GetMapping("/admin/statistics")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_MESSAGE_ADMIN)
    public ApiResponse<MessageStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.ok(topbizMessageGatewayService.getStatistics(startTime, endTime));
    }

    @GetMapping("/internal/runtime")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<RuntimeOverviewResponse> runtime() {
        return ApiResponse.ok(topbizMessageGatewayService.runtime());
    }

    @PostMapping("/internal/tasks/dispatch/run")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<TaskExecutionResponse> runDispatchTasks() {
        return ApiResponse.ok(topbizMessageGatewayService.runDispatchTasks());
    }

    @PostMapping("/internal/tasks/retry/run")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<TaskExecutionResponse> runRetryTasks() {
        return ApiResponse.ok(topbizMessageGatewayService.runRetryTasks());
    }
}
