package com.example.platform.topbiz.dto;

import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.MetricsResponse;

import java.util.List;

public record PlatformRuntimeResponse(
        MetricsResponse topbizMetrics,
        com.example.platform.message.dto.RuntimeOverviewResponse messageRuntime,
        com.example.platform.log.dto.RuntimeOverviewResponse logRuntime,
        List<AlertResponse> alerts
) {
}
