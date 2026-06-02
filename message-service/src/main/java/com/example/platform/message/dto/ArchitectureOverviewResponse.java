package com.example.platform.message.dto;

import com.example.platform.message.domain.ArchitectureOverview;

import java.util.List;

public record ArchitectureOverviewResponse(
        String service,
        String boundedContext,
        List<String> coreModules,
        List<String> layers,
        String publicBasePath
) {

    public static ArchitectureOverviewResponse from(ArchitectureOverview overview) {
        return new ArchitectureOverviewResponse(
                overview.service(),
                overview.boundedContext(),
                overview.coreModules(),
                overview.layers(),
                overview.publicBasePath()
        );
    }
}
