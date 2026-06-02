package com.example.platform.topbiz.remote;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.config.TopbizFeignConfig;
import com.example.platform.topbiz.remote.dto.RemoteAlertResponse;
import com.example.platform.topbiz.remote.dto.RemoteAlertStatusUpdateRequest;
import com.example.platform.topbiz.remote.dto.RemoteExportRequest;
import com.example.platform.topbiz.remote.dto.RemoteExportResponse;
import com.example.platform.topbiz.remote.dto.RemoteLogEntryResponse;
import com.example.platform.topbiz.remote.dto.RemoteLogIngestRequest;
import com.example.platform.topbiz.remote.dto.RemoteLogSearchResponse;
import com.example.platform.topbiz.remote.dto.RemoteMetricsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "logServiceClient", url = "${topbiz.remote.log-service.base-url}", configuration = TopbizFeignConfig.class)
public interface LogServiceClient {

    @PostMapping("/api/logs/ingest")
    ApiResponse<RemoteLogEntryResponse> ingest(@RequestBody RemoteLogIngestRequest request);

    @GetMapping("/api/logs/search")
    ApiResponse<RemoteLogSearchResponse> search(@RequestParam(required = false) String keyword);

    @GetMapping("/api/logs/trace/{traceId}")
    ApiResponse<RemoteLogSearchResponse> trace(@PathVariable String traceId);

    @GetMapping("/api/logs/metrics")
    ApiResponse<RemoteMetricsResponse> metrics(@RequestParam(required = false) String serviceName);

    @GetMapping("/api/logs/alerts")
    ApiResponse<List<RemoteAlertResponse>> alerts();

    @PostMapping("/api/logs/alerts/{alertId}/status")
    ApiResponse<Void> updateAlertStatus(@PathVariable String alertId, @RequestBody RemoteAlertStatusUpdateRequest request);

    @PostMapping("/api/logs/exports")
    ApiResponse<RemoteExportResponse> createExport(@RequestBody RemoteExportRequest request);
}
