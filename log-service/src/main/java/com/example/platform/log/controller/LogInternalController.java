package com.example.platform.log.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.log.dto.AlertRuleResponse;
import com.example.platform.log.dto.AlertRuleStatusUpdateRequest;
import com.example.platform.log.dto.AlertRuleUpsertRequest;
import com.example.platform.log.dto.ExportTaskResponse;
import com.example.platform.log.dto.InternalLogSearchRequest;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.RuntimeOverviewResponse;
import com.example.platform.log.service.LogOperationsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs/internal")
public class LogInternalController {

    private final LogOperationsService logOperationsService;

    public LogInternalController(LogOperationsService logOperationsService) {
        this.logOperationsService = logOperationsService;
    }

    @PostMapping("/search")
    public ApiResponse<LogSearchResponse> search(@Valid @RequestBody InternalLogSearchRequest request) {
        return ApiResponse.ok(logOperationsService.internalSearch(request));
    }

    @GetMapping("/runtime")
    public ApiResponse<RuntimeOverviewResponse> runtime() {
        return ApiResponse.ok(logOperationsService.runtimeOverview());
    }

    @GetMapping("/alert-rules")
    public ApiResponse<List<AlertRuleResponse>> alertRules() {
        return ApiResponse.ok(logOperationsService.listAlertRules());
    }

    @GetMapping("/exports")
    public ApiResponse<List<ExportTaskResponse>> exports() {
        return ApiResponse.ok(logOperationsService.listExportTasks());
    }

    @PostMapping("/alert-rules")
    public ApiResponse<AlertRuleResponse> upsertAlertRule(@Valid @RequestBody AlertRuleUpsertRequest request) {
        return ApiResponse.ok(logOperationsService.upsertAlertRule(request));
    }

    @PostMapping("/alert-rules/{ruleId}/enabled")
    public ApiResponse<Void> updateEnabled(@PathVariable String ruleId,
                                           @RequestBody AlertRuleStatusUpdateRequest request) {
        logOperationsService.updateAlertRuleEnabled(ruleId, request);
        return ApiResponse.ok("alert rule updated");
    }

    @DeleteMapping("/alert-rules/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable String ruleId) {
        logOperationsService.deleteAlertRule(ruleId);
        return ApiResponse.ok("alert rule deleted");
    }

    @PostMapping("/tasks/flush")
    public ApiResponse<Void> flush() {
        logOperationsService.flushQueuedLogs();
        return ApiResponse.ok("flush executed");
    }

    @PostMapping("/tasks/alerts/evaluate")
    public ApiResponse<Void> evaluateAlerts() {
        logOperationsService.evaluateAlertRules();
        return ApiResponse.ok("alert evaluation executed");
    }

    @PostMapping("/tasks/exports/run")
    public ApiResponse<Void> runExports() {
        logOperationsService.runPendingExports();
        return ApiResponse.ok("export execution finished");
    }

    @PostMapping("/tasks/exports/cleanup")
    public ApiResponse<Void> cleanupExports() {
        logOperationsService.cleanupExpiredExports();
        return ApiResponse.ok("export cleanup finished");
    }
}
