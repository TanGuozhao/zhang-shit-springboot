package com.example.platform.message.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.RuntimeOverviewResponse;
import com.example.platform.message.dto.TaskExecutionResponse;
import com.example.platform.message.service.MessageOperationsService;
import com.example.platform.message.service.MessageQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages/internal")
public class MessageInternalController {

    private final MessageQueryService messageQueryService;
    private final MessageOperationsService messageOperationsService;

    public MessageInternalController(MessageQueryService messageQueryService,
                                     MessageOperationsService messageOperationsService) {
        this.messageQueryService = messageQueryService;
        this.messageOperationsService = messageOperationsService;
    }

    @GetMapping("/runtime")
    public ApiResponse<RuntimeOverviewResponse> runtime() {
        return ApiResponse.ok(messageQueryService.getRuntimeOverview());
    }

    @PostMapping("/tasks/dispatch/run")
    public ApiResponse<TaskExecutionResponse> runDispatchTasks() {
        return ApiResponse.ok(messageOperationsService.runPendingDispatchTasks());
    }

    @PostMapping("/tasks/retry/run")
    public ApiResponse<TaskExecutionResponse> runRetryTasks() {
        return ApiResponse.ok(messageOperationsService.runPendingRetries());
    }
}
