package com.example.platform.log.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.log.dto.AlertStatusUpdateRequest;
import com.example.platform.log.dto.ExportRequest;
import com.example.platform.log.dto.ExportResponse;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.log.service.LogCommandService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogCommandController {

    private final LogCommandService logCommandService;

    public LogCommandController(LogCommandService logCommandService) {
        this.logCommandService = logCommandService;
    }

    @PostMapping("/ingest")
    public ApiResponse<LogEntryResponse> ingest(@Valid @RequestBody LogIngestRequest request) {
        return ApiResponse.ok(logCommandService.ingest(request));
    }

    @PostMapping("/alerts/{alertId}/status")
    public ApiResponse<Void> updateAlertStatus(@PathVariable String alertId,
                                               @Valid @RequestBody AlertStatusUpdateRequest request) {
        logCommandService.updateAlertStatus(alertId, request);
        return ApiResponse.ok("alert status updated");
    }

    @PostMapping("/exports")
    public ApiResponse<ExportResponse> createExport(@Valid @RequestBody ExportRequest request) {
        return ApiResponse.ok(logCommandService.createExport(request));
    }
}
