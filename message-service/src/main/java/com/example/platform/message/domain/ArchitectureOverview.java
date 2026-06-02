package com.example.platform.message.domain;

import java.util.List;

public record ArchitectureOverview(
        String service,
        String boundedContext,
        List<String> coreModules,
        List<String> layers,
        String publicBasePath
) {
}
