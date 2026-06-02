package com.example.platform.message.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.service.MessageQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageQueryController {

    private final MessageQueryService messageQueryService;

    public MessageQueryController(MessageQueryService messageQueryService) {
        this.messageQueryService = messageQueryService;
    }

    @GetMapping("/{messageId}")
    public ApiResponse<MessageResponse> getMessage(@PathVariable String messageId) {
        return ApiResponse.ok(messageQueryService.getMessage(messageId));
    }

    @GetMapping("/{messageId}/status")
    public ApiResponse<MessageStatusResponse> getStatus(@PathVariable String messageId) {
        return ApiResponse.ok(messageQueryService.getStatus(messageId));
    }

    @GetMapping("/templates")
    public ApiResponse<List<TemplateSummaryResponse>> listTemplates(@RequestParam(required = false) String channel) {
        return ApiResponse.ok(messageQueryService.listTemplates(channel));
    }

    @GetMapping("/templates/{templateCode}")
    public ApiResponse<TemplateDetailResponse> getTemplate(@PathVariable String templateCode) {
        return ApiResponse.ok(messageQueryService.getTemplate(templateCode));
    }
}
