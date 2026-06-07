package com.example.platform.message.controller;

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
import com.example.platform.message.service.MessageAdminService;
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
@RequestMapping("/api/messages/admin")
public class MessageAdminController {

    private final MessageAdminService messageAdminService;

    public MessageAdminController(MessageAdminService messageAdminService) {
        this.messageAdminService = messageAdminService;
    }

    @PostMapping("/templates")
    public ApiResponse<TemplateDetailResponse> upsertTemplate(@RequestBody TemplateUpsertRequest request) {
        return ApiResponse.ok(messageAdminService.upsertTemplate(request));
    }

    @PutMapping("/templates/{templateCode}/status")
    public ApiResponse<TemplateDetailResponse> updateTemplateStatus(@PathVariable String templateCode,
                                                                    @RequestBody TemplateStatusUpdateRequest request) {
        return ApiResponse.ok(messageAdminService.updateTemplateStatus(templateCode, request));
    }

    @PostMapping("/variables")
    public ApiResponse<VariableDefinitionResponse> upsertVariable(@RequestBody VariableUpsertRequest request) {
        return ApiResponse.ok(messageAdminService.upsertVariable(request));
    }

    @PutMapping("/variables/{variableCode}/type")
    public ApiResponse<VariableDefinitionResponse> updateVariableType(@PathVariable String variableCode,
                                                                      @RequestBody VariableTypeUpdateRequest request) {
        return ApiResponse.ok(messageAdminService.updateVariableType(variableCode, request));
    }

    @PutMapping("/variables/{variableCode}/required")
    public ApiResponse<VariableDefinitionResponse> updateVariableRequired(@PathVariable String variableCode,
                                                                          @RequestBody VariableRequiredUpdateRequest request) {
        return ApiResponse.ok(messageAdminService.updateVariableRequired(variableCode, request));
    }

    @PostMapping("/carrier/accounts")
    public ApiResponse<CarrierAccountResponse> createCarrierAccount(@RequestBody CarrierAccountRequest request) {
        return ApiResponse.ok(messageAdminService.createCarrierAccount(request));
    }

    @PostMapping("/channels/config")
    public ApiResponse<ChannelResponse> configureChannel(@RequestBody ChannelConfigRequest request) {
        return ApiResponse.ok(messageAdminService.configureChannel(request));
    }

    @PutMapping("/channels/{channelCode}/sender")
    public ApiResponse<ChannelResponse> updateChannelSender(@PathVariable String channelCode,
                                                            @RequestBody ChannelSenderUpdateRequest request) {
        return ApiResponse.ok(messageAdminService.updateChannelSender(channelCode, request));
    }

    @PostMapping("/schedule/policies")
    public ApiResponse<SchedulePolicyResponse> createSchedulePolicy(@RequestBody SchedulePolicyRequest request) {
        return ApiResponse.ok(messageAdminService.createSchedulePolicy(request));
    }

    @PostMapping("/dispatch/tasks")
    public ApiResponse<DispatchTaskResponse> createDispatchTask(@RequestBody DispatchTaskRequest request) {
        return ApiResponse.ok(messageAdminService.createDispatchTask(request));
    }

    @GetMapping("/dispatch/tasks")
    public ApiResponse<DispatchTaskListResponse> listDispatchTasks(@RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.ok(messageAdminService.listDispatchTasks(status, startTime, endTime));
    }

    @PutMapping("/dispatch/tasks/{taskId}/time")
    public ApiResponse<DispatchTaskResponse> updateDispatchTaskTime(@PathVariable String taskId,
                                                                    @RequestBody DispatchTaskTimeUpdateRequest request) {
        return ApiResponse.ok(messageAdminService.updateDispatchTaskTime(taskId, request));
    }

    @PutMapping("/dispatch/tasks/{taskId}/cancel")
    public ApiResponse<DispatchTaskResponse> cancelDispatchTask(@PathVariable String taskId) {
        return ApiResponse.ok(messageAdminService.cancelDispatchTask(taskId));
    }

    @PostMapping("/dispatch/tasks/{taskId}/trigger")
    public ApiResponse<DispatchTaskResponse> triggerDispatchTask(@PathVariable String taskId) {
        return ApiResponse.ok(messageAdminService.triggerDispatchTask(taskId));
    }

    @PostMapping("/retries/run")
    public ApiResponse<TaskExecutionResponse> runRetries() {
        return ApiResponse.ok(messageAdminService.runRetries());
    }

    @GetMapping("/statistics")
    public ApiResponse<MessageStatisticsResponse> getStatistics(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.ok(messageAdminService.getStatistics(startTime, endTime));
    }
}
