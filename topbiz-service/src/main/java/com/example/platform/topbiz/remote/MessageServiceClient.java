package com.example.platform.topbiz.remote;

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
import com.example.platform.message.dto.InboxListResponse;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.MessageErrorResponse;
import com.example.platform.message.dto.MessageRecordResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.dto.MessageStatisticsResponse;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.ReceiverResolutionRequest;
import com.example.platform.message.dto.ReceiverResolutionResponse;
import com.example.platform.message.dto.RuntimeOverviewResponse;
import com.example.platform.message.dto.SchedulePolicyRequest;
import com.example.platform.message.dto.SchedulePolicyResponse;
import com.example.platform.message.dto.ScheduleValidationRequest;
import com.example.platform.message.dto.ScheduleValidationResponse;
import com.example.platform.message.dto.TaskExecutionResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplatePreviewRequest;
import com.example.platform.message.dto.TemplatePreviewResponse;
import com.example.platform.message.dto.TemplateStatusUpdateRequest;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.dto.TemplateUpsertRequest;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.dto.VariableFillRequest;
import com.example.platform.message.dto.VariableFillResponse;
import com.example.platform.message.dto.VariableRequiredUpdateRequest;
import com.example.platform.message.dto.VariableTypeUpdateRequest;
import com.example.platform.message.dto.VariableUpsertRequest;
import com.example.platform.message.dto.VariableValidationRequest;
import com.example.platform.message.dto.VariableValidationResponse;
import com.example.platform.topbiz.config.TopbizFeignConfig;
import com.example.platform.topbiz.remote.dto.RemoteArchitectureOverviewResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@FeignClient(name = "messageServiceClient", url = "${topbiz.remote.message-service.base-url}", configuration = TopbizFeignConfig.class)
public interface MessageServiceClient {

    @GetMapping("/internal/architecture/overview")
    ApiResponse<RemoteArchitectureOverviewResponse> architectureOverview();

    @PostMapping("/api/messages/send")
    ApiResponse<MessageResponse> send(@RequestBody MessageSendRequest request);

    @PostMapping("/api/messages/drafts")
    ApiResponse<MessageResponse> saveDraft(@RequestBody MessageDraftRequest request);

    @PostMapping("/api/messages/templates/{templateCode}/preview")
    ApiResponse<TemplatePreviewResponse> preview(@PathVariable String templateCode, @RequestBody TemplatePreviewRequest request);

    @PostMapping("/api/messages/variables/fill")
    ApiResponse<VariableFillResponse> fillVariables(@RequestBody VariableFillRequest request);

    @PostMapping("/api/messages/variables/validate")
    ApiResponse<VariableValidationResponse> validateVariables(@RequestBody VariableValidationRequest request);

    @PostMapping("/api/messages/schedule/validate")
    ApiResponse<ScheduleValidationResponse> validateSchedule(@RequestBody ScheduleValidationRequest request);

    @PostMapping("/api/messages/receivers/resolve")
    ApiResponse<ReceiverResolutionResponse> resolveReceivers(
            @RequestBody ReceiverResolutionRequest request,
            @RequestParam(defaultValue = "EMAIL") String channel);

    @PutMapping("/api/messages/inbox/{inboxId}/read")
    ApiResponse<InboxMessageResponse> markRead(@PathVariable String inboxId);

    @GetMapping("/api/messages/{messageId}")
    ApiResponse<MessageResponse> getMessage(@PathVariable String messageId);

    @GetMapping("/api/messages/{messageId}/status")
    ApiResponse<MessageStatusResponse> getStatus(@PathVariable String messageId);

    @GetMapping("/api/messages/{messageId}/error")
    ApiResponse<MessageErrorResponse> getError(@PathVariable String messageId);

    @GetMapping("/api/messages/templates")
    ApiResponse<List<TemplateSummaryResponse>> listTemplates(@RequestParam(required = false) String channel);

    @GetMapping("/api/messages/templates/{templateCode}")
    ApiResponse<TemplateDetailResponse> getTemplate(@PathVariable String templateCode);

    @GetMapping("/api/messages/templates/{templateCode}/variables")
    ApiResponse<List<VariableDefinitionResponse>> getTemplateVariables(@PathVariable String templateCode);

    @GetMapping("/api/messages/channels")
    ApiResponse<List<ChannelResponse>> listChannels();

    @GetMapping("/api/messages/records")
    ApiResponse<MessageRecordResponse> listRecords(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime);

    @GetMapping("/api/messages/search")
    ApiResponse<MessageRecordResponse> search(@RequestParam String keyword);

    @GetMapping("/api/messages/inbox")
    ApiResponse<InboxListResponse> listInbox(@RequestParam(required = false) String receiver);

    @GetMapping("/api/messages/inbox/{inboxId}")
    ApiResponse<InboxMessageResponse> getInboxMessage(@PathVariable String inboxId);

    @PostMapping("/api/messages/admin/templates")
    ApiResponse<TemplateDetailResponse> upsertTemplate(@RequestBody TemplateUpsertRequest request);

    @PutMapping("/api/messages/admin/templates/{templateCode}/status")
    ApiResponse<TemplateDetailResponse> updateTemplateStatus(
            @PathVariable String templateCode,
            @RequestBody TemplateStatusUpdateRequest request);

    @PostMapping("/api/messages/admin/variables")
    ApiResponse<VariableDefinitionResponse> upsertVariable(@RequestBody VariableUpsertRequest request);

    @PutMapping("/api/messages/admin/variables/{variableCode}/type")
    ApiResponse<VariableDefinitionResponse> updateVariableType(
            @PathVariable String variableCode,
            @RequestBody VariableTypeUpdateRequest request);

    @PutMapping("/api/messages/admin/variables/{variableCode}/required")
    ApiResponse<VariableDefinitionResponse> updateVariableRequired(
            @PathVariable String variableCode,
            @RequestBody VariableRequiredUpdateRequest request);

    @PostMapping("/api/messages/admin/carrier/accounts")
    ApiResponse<CarrierAccountResponse> createCarrierAccount(@RequestBody CarrierAccountRequest request);

    @PostMapping("/api/messages/admin/channels/config")
    ApiResponse<ChannelResponse> configureChannel(@RequestBody ChannelConfigRequest request);

    @PutMapping("/api/messages/admin/channels/{channelCode}/sender")
    ApiResponse<ChannelResponse> updateChannelSender(
            @PathVariable String channelCode,
            @RequestBody ChannelSenderUpdateRequest request);

    @PostMapping("/api/messages/admin/schedule/policies")
    ApiResponse<SchedulePolicyResponse> createSchedulePolicy(@RequestBody SchedulePolicyRequest request);

    @PostMapping("/api/messages/admin/dispatch/tasks")
    ApiResponse<DispatchTaskResponse> createDispatchTask(@RequestBody DispatchTaskRequest request);

    @GetMapping("/api/messages/admin/dispatch/tasks")
    ApiResponse<DispatchTaskListResponse> listDispatchTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime);

    @PutMapping("/api/messages/admin/dispatch/tasks/{taskId}/time")
    ApiResponse<DispatchTaskResponse> updateDispatchTaskTime(
            @PathVariable String taskId,
            @RequestBody DispatchTaskTimeUpdateRequest request);

    @PutMapping("/api/messages/admin/dispatch/tasks/{taskId}/cancel")
    ApiResponse<DispatchTaskResponse> cancelDispatchTask(@PathVariable String taskId);

    @PostMapping("/api/messages/admin/dispatch/tasks/{taskId}/trigger")
    ApiResponse<DispatchTaskResponse> triggerDispatchTask(@PathVariable String taskId);

    @PostMapping("/api/messages/admin/retries/run")
    ApiResponse<TaskExecutionResponse> runRetries();

    @GetMapping("/api/messages/admin/statistics")
    ApiResponse<MessageStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime);

    @GetMapping("/api/messages/internal/runtime")
    ApiResponse<RuntimeOverviewResponse> runtime();

    @PostMapping("/api/messages/internal/tasks/dispatch/run")
    ApiResponse<TaskExecutionResponse> runDispatchTasks();

    @PostMapping("/api/messages/internal/tasks/retry/run")
    ApiResponse<TaskExecutionResponse> runRetryTasks();
}
