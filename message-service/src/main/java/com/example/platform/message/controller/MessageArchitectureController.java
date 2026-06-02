package com.example.platform.message.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.message.dto.ArchitectureOverviewResponse;
import com.example.platform.message.service.MessageArchitectureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/architecture")
public class MessageArchitectureController {

    private final MessageArchitectureService messageArchitectureService;

    public MessageArchitectureController(MessageArchitectureService messageArchitectureService) {
        this.messageArchitectureService = messageArchitectureService;
    }

    @GetMapping("/overview")
    public ApiResponse<ArchitectureOverviewResponse> overview() {
        return ApiResponse.ok(messageArchitectureService.overview());
    }
}
