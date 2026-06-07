package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.ChannelResponse;
import com.example.platform.message.dto.InboxListResponse;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.MessageErrorResponse;
import com.example.platform.message.dto.MessageRecordResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.ReceiverResolutionRequest;
import com.example.platform.message.dto.ReceiverResolutionResponse;
import com.example.platform.message.dto.ScheduleValidationRequest;
import com.example.platform.message.dto.ScheduleValidationResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplatePreviewRequest;
import com.example.platform.message.dto.TemplatePreviewResponse;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.dto.VariableFillRequest;
import com.example.platform.message.dto.VariableFillResponse;
import com.example.platform.message.dto.VariableValidationRequest;
import com.example.platform.message.dto.VariableValidationResponse;
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
import java.util.List;

@RestController
@RequestMapping("/api/topbiz/messages")
public class TopbizMessageController {

    private final TopbizMessageGatewayService topbizMessageGatewayService;

    public TopbizMessageController(TopbizMessageGatewayService topbizMessageGatewayService) {
        this.topbizMessageGatewayService = topbizMessageGatewayService;
    }

    @PostMapping("/send")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageResponse> send(@Valid @RequestBody MessageSendRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.send(request));
    }

    @PostMapping("/drafts")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageResponse> saveDraft(@Valid @RequestBody MessageDraftRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.saveDraft(request));
    }

    @PostMapping("/templates/{templateCode}/preview")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<TemplatePreviewResponse> preview(@PathVariable String templateCode,
                                                        @RequestBody TemplatePreviewRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.preview(templateCode, request));
    }

    @PostMapping("/variables/fill")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<VariableFillResponse> fillVariables(@Valid @RequestBody VariableFillRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.fillVariables(request));
    }

    @PostMapping("/variables/validate")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<VariableValidationResponse> validateVariables(@Valid @RequestBody VariableValidationRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.validateVariables(request));
    }

    @PostMapping("/schedule/validate")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<ScheduleValidationResponse> validateSchedule(@RequestBody ScheduleValidationRequest request) {
        return ApiResponse.ok(topbizMessageGatewayService.validateSchedule(request));
    }

    @PostMapping("/receivers/resolve")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<ReceiverResolutionResponse> resolveReceivers(
            @RequestBody ReceiverResolutionRequest request,
            @RequestParam(defaultValue = "EMAIL") String channel) {
        return ApiResponse.ok(topbizMessageGatewayService.resolveReceivers(request, channel));
    }

    @PutMapping("/inbox/{inboxId}/read")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<InboxMessageResponse> markRead(@PathVariable String inboxId) {
        return ApiResponse.ok(topbizMessageGatewayService.markRead(inboxId));
    }

    @GetMapping("/{messageId}")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageResponse> getMessage(@PathVariable String messageId) {
        return ApiResponse.ok(topbizMessageGatewayService.getMessage(messageId));
    }

    @GetMapping("/{messageId}/status")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageStatusResponse> getStatus(@PathVariable String messageId) {
        return ApiResponse.ok(topbizMessageGatewayService.getStatus(messageId));
    }

    @GetMapping("/{messageId}/error")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageErrorResponse> getError(@PathVariable String messageId) {
        return ApiResponse.ok(topbizMessageGatewayService.getError(messageId));
    }

    @GetMapping("/templates")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<List<TemplateSummaryResponse>> listTemplates(@RequestParam(required = false) String channel) {
        return ApiResponse.ok(topbizMessageGatewayService.listTemplates(channel));
    }

    @GetMapping("/templates/{templateCode}")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<TemplateDetailResponse> getTemplate(@PathVariable String templateCode) {
        return ApiResponse.ok(topbizMessageGatewayService.getTemplate(templateCode));
    }

    @GetMapping("/templates/{templateCode}/variables")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<List<VariableDefinitionResponse>> getTemplateVariables(@PathVariable String templateCode) {
        return ApiResponse.ok(topbizMessageGatewayService.getTemplateVariables(templateCode));
    }

    @GetMapping("/channels")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<List<ChannelResponse>> listChannels() {
        return ApiResponse.ok(topbizMessageGatewayService.listChannels());
    }

    @GetMapping("/records")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageRecordResponse> listRecords(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.ok(topbizMessageGatewayService.listRecords(status, channel, keyword, receiver, startTime, endTime));
    }

    @GetMapping("/search")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<MessageRecordResponse> search(@RequestParam String keyword) {
        return ApiResponse.ok(topbizMessageGatewayService.search(keyword));
    }

    @GetMapping("/inbox")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<InboxListResponse> listInbox(@RequestParam(required = false) String receiver) {
        return ApiResponse.ok(topbizMessageGatewayService.listInbox(receiver));
    }

    @GetMapping("/inbox/{inboxId}")
    @RequiresPermissions(TopbizPermissions.MESSAGE_ACCESS)
    public ApiResponse<InboxMessageResponse> getInboxMessage(@PathVariable String inboxId) {
        return ApiResponse.ok(topbizMessageGatewayService.getInboxMessage(inboxId));
    }
}
