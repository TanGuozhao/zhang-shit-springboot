package com.example.platform.topbiz.dto;

import com.example.platform.topbiz.remote.dto.RemoteArchitectureOverviewResponse;

public record PlatformArchitectureResponse(
        RemoteArchitectureOverviewResponse topbiz,
        RemoteArchitectureOverviewResponse userService,
        RemoteArchitectureOverviewResponse messageService,
        RemoteArchitectureOverviewResponse logService
) {
}
