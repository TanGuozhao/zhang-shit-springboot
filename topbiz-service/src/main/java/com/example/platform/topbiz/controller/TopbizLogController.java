package com.example.platform.topbiz.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.AlertRuleResponse;
import com.example.platform.log.dto.AlertRuleStatusUpdateRequest;
import com.example.platform.log.dto.AlertRuleUpsertRequest;
import com.example.platform.log.dto.AlertStatusUpdateRequest;
import com.example.platform.log.dto.ExportRequest;
import com.example.platform.log.dto.ExportResponse;
import com.example.platform.log.dto.ExportTaskResponse;
import com.example.platform.log.dto.InternalLogSearchRequest;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.log.dto.RuntimeOverviewResponse;
import com.example.platform.topbiz.security.TopbizPermissions;
import com.example.platform.topbiz.service.TopbizLogGatewayService;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topbiz/logs")
public class TopbizLogController {

    private final TopbizLogGatewayService topbizLogGatewayService;

    public TopbizLogController(TopbizLogGatewayService topbizLogGatewayService) {
        this.topbizLogGatewayService = topbizLogGatewayService;
    }

    @PostMapping("/ingest")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<LogEntryResponse> ingest(@Valid @RequestBody LogIngestRequest request) {
        return ApiResponse.ok(topbizLogGatewayService.ingest(request));
    }

    @GetMapping("/search")
    @RequiresPermissions(TopbizPermissions.LOG_QUERY)
    public ApiResponse<LogSearchResponse> search(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(topbizLogGatewayService.search(keyword));
    }

    @GetMapping("/trace/{traceId}")
    @RequiresPermissions(TopbizPermissions.LOG_QUERY)
    public ApiResponse<LogSearchResponse> trace(@PathVariable String traceId) {
        return ApiResponse.ok(topbizLogGatewayService.trace(traceId));
    }

    @GetMapping("/metrics")
    @RequiresPermissions(TopbizPermissions.LOG_QUERY)
    public ApiResponse<MetricsResponse> metrics(@RequestParam(required = false) String serviceName) {
        return ApiResponse.ok(topbizLogGatewayService.metrics(serviceName));
    }

    @GetMapping("/alerts")
    @RequiresPermissions(TopbizPermissions.LOG_QUERY)
    public ApiResponse<List<AlertResponse>> alerts() {
        return ApiResponse.ok(topbizLogGatewayService.alerts());
    }

    @PostMapping("/alerts/{alertId}/status")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<Void> updateAlertStatus(@PathVariable String alertId,
                                               @Valid @RequestBody AlertStatusUpdateRequest request) {
        topbizLogGatewayService.updateAlertStatus(alertId, request);
        return ApiResponse.ok("alert status updated");
    }

    @PostMapping("/exports")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<ExportResponse> createExport(@Valid @RequestBody ExportRequest request) {
        return ApiResponse.ok(topbizLogGatewayService.createExport(request));
    }

    @PostMapping("/internal/search")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<LogSearchResponse> internalSearch(@Valid @RequestBody InternalLogSearchRequest request) {
        return ApiResponse.ok(topbizLogGatewayService.internalSearch(request));
    }

    @GetMapping("/internal/runtime")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<RuntimeOverviewResponse> runtime() {
        return ApiResponse.ok(topbizLogGatewayService.runtime());
    }

    @GetMapping("/internal/alert-rules")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<List<AlertRuleResponse>> alertRules() {
        return ApiResponse.ok(topbizLogGatewayService.alertRules());
    }

    @GetMapping("/internal/exports")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<List<ExportTaskResponse>> exports() {
        return ApiResponse.ok(topbizLogGatewayService.exports());
    }

    @PostMapping("/internal/alert-rules")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<AlertRuleResponse> upsertAlertRule(@Valid @RequestBody AlertRuleUpsertRequest request) {
        return ApiResponse.ok(topbizLogGatewayService.upsertAlertRule(request));
    }

    @PostMapping("/internal/alert-rules/{ruleId}/enabled")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<Void> updateAlertRuleEnabled(@PathVariable String ruleId,
                                                    @RequestBody AlertRuleStatusUpdateRequest request) {
        topbizLogGatewayService.updateAlertRuleEnabled(ruleId, request);
        return ApiResponse.ok("alert rule updated");
    }

    @DeleteMapping("/internal/alert-rules/{ruleId}")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_LOG_ADMIN)
    public ApiResponse<Void> deleteRule(@PathVariable String ruleId) {
        topbizLogGatewayService.deleteRule(ruleId);
        return ApiResponse.ok("alert rule deleted");
    }

    @PostMapping("/internal/tasks/flush")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<Void> flush() {
        topbizLogGatewayService.flush();
        return ApiResponse.ok("flush executed");
    }

    @PostMapping("/internal/tasks/alerts/evaluate")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<Void> evaluateAlerts() {
        topbizLogGatewayService.evaluateAlerts();
        return ApiResponse.ok("alert evaluation executed");
    }

    @PostMapping("/internal/tasks/exports/run")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<Void> runExports() {
        topbizLogGatewayService.runExports();
        return ApiResponse.ok("export execution finished");
    }

    @PostMapping("/internal/tasks/exports/cleanup")
    @RequiresPermissions(TopbizPermissions.TOPBIZ_RUNTIME_OPERATE)
    public ApiResponse<Void> cleanupExports() {
        topbizLogGatewayService.cleanupExports();
        return ApiResponse.ok("export cleanup finished");
    }
}
