package com.example.platform.log.service;

import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.ArchitectureOverview;
import com.example.platform.log.dto.ArchitectureOverviewResponse;
import com.example.platform.log.repository.LogModuleRepository;
import org.springframework.stereotype.Service;

@Service
public class LogArchitectureService {

    private final LogModuleRepository logModuleRepository;
    private final LogServiceProperties logServiceProperties;

    public LogArchitectureService(LogModuleRepository logModuleRepository,
                                  LogServiceProperties logServiceProperties) {
        this.logModuleRepository = logModuleRepository;
        this.logServiceProperties = logServiceProperties;
    }

    public ArchitectureOverviewResponse overview() {
        ArchitectureOverview overview = new ArchitectureOverview(
                "log-service",
                logServiceProperties.boundedContext(),
                logModuleRepository.coreModules(),
                logModuleRepository.layers(),
                logServiceProperties.publicBasePath()
        );
        return ArchitectureOverviewResponse.from(overview);
    }
}
