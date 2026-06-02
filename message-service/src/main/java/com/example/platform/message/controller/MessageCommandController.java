package com.example.platform.message.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.service.MessageCommandService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ApiResponse<com.example.platform.message.dto.TemplatePreviewResponse> preview(@PathVariable String templateCode,
                                                                                         @RequestBody com.example.platform.message.dto.TemplatePreviewRequest request) {
        return ApiResponse.ok(messageCommandService.preview(templateCode, request));
    }
}
