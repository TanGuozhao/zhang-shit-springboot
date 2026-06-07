package com.example.platform.log.service;

import com.example.platform.log.config.LogServiceProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogBackgroundScheduler {

    private final LogServiceProperties properties;
    private final LogOperationsService logOperationsService;

    public LogBackgroundScheduler(LogServiceProperties properties, LogOperationsService logOperationsService) {
        this.properties = properties;
        this.logOperationsService = logOperationsService;
    }

    @Scheduled(fixedDelayString = "${platform.log.buffer.flush-interval-ms:5000}")
    public void flush() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        try {
            logOperationsService.flushQueuedLogs();
        } catch (RuntimeException ignored) {
        }
    }

    @Scheduled(fixedDelayString = "${platform.log.scheduler.alert-evaluation-interval-ms:10000}")
    public void evaluateAlerts() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        logOperationsService.evaluateAlertRules();
    }

    @Scheduled(fixedDelayString = "${platform.log.scheduler.export-interval-ms:15000}")
    public void runExports() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        logOperationsService.runPendingExports();
    }

    @Scheduled(fixedDelayString = "${platform.log.scheduler.cleanup-interval-ms:3600000}")
    public void cleanupExports() {
        if (!properties.scheduler().enabled()) {
            return;
        }
        logOperationsService.cleanupExpiredExports();
    }
}
