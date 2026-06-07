package com.example.platform.message.service;

import com.example.platform.message.config.MessageServiceProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageBackgroundScheduler {

    private final MessageServiceProperties messageServiceProperties;
    private final MessageOperationsService messageOperationsService;

    public MessageBackgroundScheduler(MessageServiceProperties messageServiceProperties,
                                      MessageOperationsService messageOperationsService) {
        this.messageServiceProperties = messageServiceProperties;
        this.messageOperationsService = messageOperationsService;
    }

    @Scheduled(fixedDelayString = "30000")
    public void runDispatchLoop() {
        if (messageServiceProperties.schedulerEnabled()) {
            messageOperationsService.runPendingDispatchTasks();
        }
    }

    @Scheduled(fixedDelayString = "45000")
    public void runRetryLoop() {
        if (messageServiceProperties.retryEnabled()) {
            messageOperationsService.runPendingRetries();
        }
    }
}
