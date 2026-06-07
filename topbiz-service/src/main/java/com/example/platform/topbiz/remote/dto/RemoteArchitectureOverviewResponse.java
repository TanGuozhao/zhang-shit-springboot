package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemoteArchitectureOverviewResponse(
        String service,
        String boundedContext,
        List<String> coreModules,
        List<String> layers,
        String publicBasePath
) {
}
