package com.example.platform.log.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.log.service.LogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogQueryController {

    private final LogQueryService logQueryService;

    public LogQueryController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping("/search")
    public ApiResponse<LogSearchResponse> search(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(logQueryService.search(keyword));
    }

    @GetMapping("/trace/{traceId}")
    public ApiResponse<LogSearchResponse> trace(@PathVariable String traceId) {
        return ApiResponse.ok(logQueryService.trace(traceId));
    }

    @GetMapping("/metrics")
    public ApiResponse<MetricsResponse> metrics(@RequestParam(required = false, defaultValue = "topbiz") String serviceName) {
        return ApiResponse.ok(logQueryService.metrics(serviceName));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertResponse>> alerts() {
        return ApiResponse.ok(logQueryService.alerts());
    }
}
