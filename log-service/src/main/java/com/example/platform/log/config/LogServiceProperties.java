package com.example.platform.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "platform.log")
public record LogServiceProperties(
        String boundedContext,
        String publicBasePath,
        StorageProperties storage,
        BufferProperties buffer,
        SchedulerProperties scheduler,
        NotificationProperties notification,
        ExportProperties export,
        SearchProperties search,
        IngestProperties ingest,
        MaskingProperties masking
) {

    public record StorageProperties(
            boolean clickhouseEnabled,
            String clickhouseUrl,
            String clickhouseUsername,
            String clickhousePassword,
            String clickhouseTable,
            int retentionDays,
            String failedBatchDirectory,
            int maxFlushRetries
    ) {
    }

    public record BufferProperties(
            int capacity,
            int batchSize,
            long flushIntervalMs
    ) {
    }

    public record SchedulerProperties(
            boolean enabled,
            long alertEvaluationIntervalMs,
            long exportIntervalMs,
            long cleanupIntervalMs
    ) {
    }

    public record NotificationProperties(
            boolean consoleEnabled,
            boolean webhookEnabled,
            String webhookUrl
    ) {
    }

    public record ExportProperties(
            String directory,
            int defaultWindowMinutes,
            int maxRows,
            long fileTtlHours
    ) {
    }

    public record SearchProperties(
            int defaultWindowHours,
            int maxWindowDays,
            int maxKeywordLength,
            int maxPageSize
    ) {
    }

    public record IngestProperties(
            int maxFutureSkewSeconds,
            int maxPastDays,
            int maxMessageLength,
            List<String> allowedLevels
    ) {
    }

    public record MaskingProperties(
            boolean enabled
    ) {
    }
}
