package com.example.platform.user.service;

import com.example.platform.user.config.UserServiceProperties;
import com.example.platform.user.domain.ArchitectureOverview;
import com.example.platform.user.dto.ArchitectureOverviewResponse;
import com.example.platform.user.repository.UserModuleRepository;
import org.springframework.stereotype.Service;

@Service
public class UserArchitectureService {

    private final UserModuleRepository userModuleRepository;
    private final UserServiceProperties userServiceProperties;

    public UserArchitectureService(UserModuleRepository userModuleRepository,
                                   UserServiceProperties userServiceProperties) {
        this.userModuleRepository = userModuleRepository;
        this.userServiceProperties = userServiceProperties;
    }

    public ArchitectureOverviewResponse overview() {
        ArchitectureOverview overview = new ArchitectureOverview(
                "user-service",
                userServiceProperties.boundedContext(),
                userModuleRepository.coreModules(),
                userModuleRepository.layers(),
                userServiceProperties.publicBasePath()
        );
        return ArchitectureOverviewResponse.from(overview);
    }
}
