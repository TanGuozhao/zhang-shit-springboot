package com.example.platform.message.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.ChannelResponse;
import com.example.platform.message.dto.InboxListResponse;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageErrorResponse;
import com.example.platform.message.dto.MessageRecordResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.service.MessageQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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

    @GetMapping("/{messageId}/error")
    public ApiResponse<MessageErrorResponse> getError(@PathVariable String messageId) {
        return ApiResponse.ok(messageQueryService.getError(messageId));
    }

    @GetMapping("/templates")
    public ApiResponse<List<TemplateSummaryResponse>> listTemplates(@RequestParam(required = false) String channel) {
        return ApiResponse.ok(messageQueryService.listTemplates(channel));
    }

    @GetMapping("/templates/{templateCode}")
    public ApiResponse<TemplateDetailResponse> getTemplate(@PathVariable String templateCode) {
        return ApiResponse.ok(messageQueryService.getTemplate(templateCode));
    }

    @GetMapping("/templates/{templateCode}/variables")
    public ApiResponse<List<VariableDefinitionResponse>> getTemplateVariables(@PathVariable String templateCode) {
        return ApiResponse.ok(messageQueryService.getTemplateVariables(templateCode));
    }

    @GetMapping("/channels")
    public ApiResponse<List<ChannelResponse>> listChannels() {
        return ApiResponse.ok(messageQueryService.listChannels());
    }

    @GetMapping("/records")
    public ApiResponse<MessageRecordResponse> listRecords(@RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String channel,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String receiver,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        return ApiResponse.ok(messageQueryService.listRecords(status, channel, keyword, receiver, startTime, endTime));
    }

    @GetMapping("/search")
    public ApiResponse<MessageRecordResponse> search(@RequestParam String keyword) {
        return ApiResponse.ok(messageQueryService.listRecords(null, null, keyword, null, null, null));
    }

    @GetMapping("/inbox")
    public ApiResponse<InboxListResponse> listInbox(@RequestParam(required = false) String receiver) {
        return ApiResponse.ok(messageQueryService.listInbox(receiver));
    }

    @GetMapping("/inbox/{inboxId}")
    public ApiResponse<InboxMessageResponse> getInboxMessage(@PathVariable String inboxId) {
        return ApiResponse.ok(messageQueryService.getInboxMessage(inboxId));
    }
}
