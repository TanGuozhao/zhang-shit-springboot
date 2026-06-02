package com.example.platform.message.service;

import com.example.platform.message.config.MessageServiceProperties;
import com.example.platform.message.domain.ArchitectureOverview;
import com.example.platform.message.dto.ArchitectureOverviewResponse;
import com.example.platform.message.repository.MessageModuleRepository;
import org.springframework.stereotype.Service;

@Service
public class MessageArchitectureService {

    private final MessageModuleRepository messageModuleRepository;
    private final MessageServiceProperties messageServiceProperties;

    public MessageArchitectureService(MessageModuleRepository messageModuleRepository,
                                      MessageServiceProperties messageServiceProperties) {
        this.messageModuleRepository = messageModuleRepository;
        this.messageServiceProperties = messageServiceProperties;
    }

    public ArchitectureOverviewResponse overview() {
        ArchitectureOverview overview = new ArchitectureOverview(
                "message-service",
                messageServiceProperties.boundedContext(),
                messageModuleRepository.coreModules(),
                messageModuleRepository.layers(),
                messageServiceProperties.publicBasePath()
        );
        return ArchitectureOverviewResponse.from(overview);
    }
}
