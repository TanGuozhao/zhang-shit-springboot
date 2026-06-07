package com.example.platform.topbiz.remote;

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
import com.example.platform.topbiz.config.TopbizFeignConfig;
import com.example.platform.topbiz.remote.dto.RemoteArchitectureOverviewResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "logServiceClient", url = "${topbiz.remote.log-service.base-url}", configuration = TopbizFeignConfig.class)
public interface LogServiceClient {

    @GetMapping("/internal/architecture/overview")
    ApiResponse<RemoteArchitectureOverviewResponse> architectureOverview();

    @PostMapping("/api/logs/ingest")
    ApiResponse<LogEntryResponse> ingest(@RequestBody LogIngestRequest request);

    @GetMapping("/api/logs/search")
    ApiResponse<LogSearchResponse> search(@RequestParam(required = false) String keyword);

    @GetMapping("/api/logs/trace/{traceId}")
    ApiResponse<LogSearchResponse> trace(@PathVariable String traceId);

    @GetMapping("/api/logs/metrics")
    ApiResponse<MetricsResponse> metrics(@RequestParam(required = false) String serviceName);

    @GetMapping("/api/logs/alerts")
    ApiResponse<List<AlertResponse>> alerts();

    @PostMapping("/api/logs/alerts/{alertId}/status")
    ApiResponse<Void> updateAlertStatus(@PathVariable String alertId, @RequestBody AlertStatusUpdateRequest request);

    @PostMapping("/api/logs/exports")
    ApiResponse<ExportResponse> createExport(@RequestBody ExportRequest request);

    @PostMapping("/api/logs/internal/search")
    ApiResponse<LogSearchResponse> internalSearch(@RequestBody InternalLogSearchRequest request);

    @GetMapping("/api/logs/internal/runtime")
    ApiResponse<RuntimeOverviewResponse> runtime();

    @GetMapping("/api/logs/internal/alert-rules")
    ApiResponse<List<AlertRuleResponse>> alertRules();

    @GetMapping("/api/logs/internal/exports")
    ApiResponse<List<ExportTaskResponse>> exports();

    @PostMapping("/api/logs/internal/alert-rules")
    ApiResponse<AlertRuleResponse> upsertAlertRule(@RequestBody AlertRuleUpsertRequest request);

    @PostMapping("/api/logs/internal/alert-rules/{ruleId}/enabled")
    ApiResponse<Void> updateAlertRuleEnabled(@PathVariable String ruleId, @RequestBody AlertRuleStatusUpdateRequest request);

    @DeleteMapping("/api/logs/internal/alert-rules/{ruleId}")
    ApiResponse<Void> deleteRule(@PathVariable String ruleId);

    @PostMapping("/api/logs/internal/tasks/flush")
    ApiResponse<Void> flush();

    @PostMapping("/api/logs/internal/tasks/alerts/evaluate")
    ApiResponse<Void> evaluateAlerts();

    @PostMapping("/api/logs/internal/tasks/exports/run")
    ApiResponse<Void> runExports();

    @PostMapping("/api/logs/internal/tasks/exports/cleanup")
    ApiResponse<Void> cleanupExports();
}
