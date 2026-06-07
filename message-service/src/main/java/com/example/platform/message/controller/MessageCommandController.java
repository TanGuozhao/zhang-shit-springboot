package com.example.platform.message.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.dto.ReceiverResolutionRequest;
import com.example.platform.message.dto.ReceiverResolutionResponse;
import com.example.platform.message.dto.ScheduleValidationRequest;
import com.example.platform.message.dto.ScheduleValidationResponse;
import com.example.platform.message.service.MessageCommandService;
import com.example.platform.message.dto.TemplatePreviewRequest;
import com.example.platform.message.dto.TemplatePreviewResponse;
import com.example.platform.message.dto.VariableFillRequest;
import com.example.platform.message.dto.VariableFillResponse;
import com.example.platform.message.dto.VariableValidationRequest;
import com.example.platform.message.dto.VariableValidationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageCommandController {

    private final MessageCommandService messageCommandService;

    public MessageCommandController(MessageCommandService messageCommandService) {
        this.messageCommandService = messageCommandService;
    }

    @PostMapping("/send")
    public ApiResponse<MessageResponse> send(@Valid @RequestBody MessageSendRequest request) {
        return ApiResponse.ok(messageCommandService.send(request));
    }

    @PostMapping("/drafts")
    public ApiResponse<MessageResponse> saveDraft(@Valid @RequestBody MessageDraftRequest request) {
        return ApiResponse.ok(messageCommandService.saveDraft(request));
    }

    @PostMapping("/templates/{templateCode}/preview")
    public ApiResponse<TemplatePreviewResponse> preview(@PathVariable String templateCode,
                                                        @RequestBody TemplatePreviewRequest request) {
        return ApiResponse.ok(messageCommandService.preview(templateCode, request));
    }

    @PostMapping("/variables/fill")
    public ApiResponse<VariableFillResponse> fillVariables(@Valid @RequestBody VariableFillRequest request) {
        return ApiResponse.ok(messageCommandService.fillVariables(request));
    }

    @PostMapping("/variables/validate")
    public ApiResponse<VariableValidationResponse> validateVariables(@Valid @RequestBody VariableValidationRequest request) {
        return ApiResponse.ok(messageCommandService.validateVariables(request));
    }

    @PostMapping("/schedule/validate")
    public ApiResponse<ScheduleValidationResponse> validateSchedule(@RequestBody ScheduleValidationRequest request) {
        return ApiResponse.ok(messageCommandService.validateSchedule(request));
    }

    @PostMapping("/receivers/resolve")
    public ApiResponse<ReceiverResolutionResponse> resolveReceivers(@RequestBody ReceiverResolutionRequest request,
                                                                    @RequestParam(defaultValue = "EMAIL") String channel) {
        return ApiResponse.ok(messageCommandService.resolveReceivers(request, channel));
    }

    @PutMapping("/inbox/{inboxId}/read")
    public ApiResponse<InboxMessageResponse> markRead(@PathVariable String inboxId) {
        return ApiResponse.ok(messageCommandService.markInboxMessageRead(inboxId));
    }
}
