package com.example.platform.log.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AccessLogRecord;
import com.example.platform.log.domain.AlertEvent;
import com.example.platform.log.domain.AlertRule;
import com.example.platform.log.domain.AlertRuleType;
import com.example.platform.log.domain.ExportTask;
import com.example.platform.log.domain.FlushReport;
import com.example.platform.log.domain.RuntimeState;
import com.example.platform.log.dto.AlertRuleResponse;
import com.example.platform.log.dto.AlertRuleStatusUpdateRequest;
import com.example.platform.log.dto.AlertRuleUpsertRequest;
import com.example.platform.log.dto.ExportTaskResponse;
import com.example.platform.log.dto.InternalLogSearchRequest;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.RuntimeOverviewResponse;
import com.example.platform.log.repository.AlertRepository;
import com.example.platform.log.repository.AlertRuleRepository;
import com.example.platform.log.repository.BufferedLogQueueRepository;
import com.example.platform.log.repository.ExportTaskRepository;
import com.example.platform.log.repository.LogEntryRepository;
import com.example.platform.log.repository.RetryLogRepository;
import com.example.platform.log.repository.RuntimeStateRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LogOperationsService {

    private final LogServiceProperties properties;
    private final BufferedLogQueueRepository bufferedLogQueueRepository;
    private final RetryLogRepository retryLogRepository;
    private final LogEntryRepository logEntryRepository;
    private final RuntimeStateRepository runtimeStateRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final ExportTaskRepository exportTaskRepository;
    private final LogQueryService logQueryService;
    private final AlertNotificationService alertNotificationService;
    private final LogDataMaskingService logDataMaskingService;

    public LogOperationsService(LogServiceProperties properties,
                                BufferedLogQueueRepository bufferedLogQueueRepository,
                                RetryLogRepository retryLogRepository,
                                LogEntryRepository logEntryRepository,
                                RuntimeStateRepository runtimeStateRepository,
                                AlertRuleRepository alertRuleRepository,
                                AlertRepository alertRepository,
                                ExportTaskRepository exportTaskRepository,
                                LogQueryService logQueryService,
                                AlertNotificationService alertNotificationService,
                                LogDataMaskingService logDataMaskingService) {
        this.properties = properties;
        this.bufferedLogQueueRepository = bufferedLogQueueRepository;
        this.retryLogRepository = retryLogRepository;
        this.logEntryRepository = logEntryRepository;
        this.runtimeStateRepository = runtimeStateRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
        this.exportTaskRepository = exportTaskRepository;
        this.logQueryService = logQueryService;
        this.alertNotificationService = alertNotificationService;
        this.logDataMaskingService = logDataMaskingService;
    }

    public FlushReport flushQueuedLogs() {
        Instant now = Instant.now();
        int batchSize = properties.buffer().batchSize();
        List<AccessLogRecord> records = new ArrayList<>();
        records.addAll(retryLogRepository.drain(batchSize));
        if (records.size() < batchSize) {
            records.addAll(bufferedLogQueueRepository.drain(batchSize - records.size()));
        }
        if (records.isEmpty()) {
            runtimeStateRepository.markFlush(now, "SKIPPED_EMPTY", null);
            return new FlushReport(true, 0, "no records to flush");
        }
        try {
            logEntryRepository.saveBatch(records);
            retryLogRepository.markSucceeded(records);
            runtimeStateRepository.markFlush(now, "SUCCESS", null);
            return new FlushReport(true, records.size(), "flushed");
        } catch (SQLException ex) {
            retryLogRepository.stashAll(records);
            List<AccessLogRecord> exhausted = retryLogRepository.findExhausted(properties.storage().maxFlushRetries());
            if (!exhausted.isEmpty()) {
                archiveFailedBatch(exhausted, ex.getMessage(), now);
                retryLogRepository.discard(exhausted);
                runtimeStateRepository.incrementArchivedFailedBatches(1);
            }
            runtimeStateRepository.markFlush(now, exhausted.isEmpty() ? "FAILED_RETRYING" : "FAILED_ARCHIVED", ex.getMessage());
            throw new BusinessException("CLICKHOUSE_WRITE_FAILED", ex.getMessage());
        }
    }

    public int evaluateAlertRules() {
        Instant now = Instant.now();
        int created = 0;
        List<String> issues = new ArrayList<>();
        for (AlertRule rule : alertRuleRepository.findEnabled()) {
            try {
                RuleEvaluationOutcome outcome = evaluateRule(rule, now);
                if (outcome.alertCreated()) {
                    created++;
                }
                if (outcome.notificationIssue() != null && !outcome.notificationIssue().isBlank()) {
                    issues.add(outcome.notificationIssue());
                }
            } catch (RuntimeException ex) {
                issues.add(ex.getMessage());
            }
        }
        String result = issues.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS";
        runtimeStateRepository.markAlertEvaluation(now, result, issues.isEmpty() ? null : String.join("; ", issues));
        return created;
    }

    public int runPendingExports() {
        ensureExportDirectory();
        Instant now = Instant.now();
        int processed = 0;
        List<String> issues = new ArrayList<>();
        for (ExportTask task : exportTaskRepository.findByStatus("QUEUED")) {
            processed++;
            exportTaskRepository.save(new ExportTask(
                    task.exportId(),
                    task.format(),
                    task.query(),
                    "RUNNING",
                    task.downloadPath(),
                    task.createdAt(),
                    null,
                    null,
                    null,
                    null
            ));
            try {
                Path filePath = buildFilePath(task);
                List<AccessLogRecord> records = selectExportRecords(task);
                if (records.size() > properties.export().maxRows()) {
                    throw new BusinessException("EXPORT_ROW_LIMIT_EXCEEDED",
                            "export result exceeds max rows %d".formatted(properties.export().maxRows()));
                }
                writeExport(task.format(), filePath, records);
                Instant completedAt = Instant.now();
                exportTaskRepository.save(new ExportTask(
                        task.exportId(),
                        task.format(),
                        task.query(),
                        "COMPLETED",
                        task.downloadPath(),
                        task.createdAt(),
                        completedAt,
                        completedAt.plus(Duration.ofHours(properties.export().fileTtlHours())),
                        records.size(),
                        null
                ));
            } catch (IOException | BusinessException ex) {
                issues.add(ex.getMessage());
                runtimeStateRepository.markError(ex.getMessage());
                exportTaskRepository.save(new ExportTask(
                        task.exportId(),
                        task.format(),
                        task.query(),
                        "FAILED",
                        task.downloadPath(),
                        task.createdAt(),
                        Instant.now(),
                        null,
                        null,
                        ex.getMessage()
                ));
            }
        }
        runtimeStateRepository.markExportRun(now,
                issues.isEmpty() ? (processed == 0 ? "SKIPPED_EMPTY" : "SUCCESS") : "PARTIAL_SUCCESS",
                issues.isEmpty() ? null : String.join("; ", issues));
        return processed;
    }

    public int cleanupExpiredExports() {
        Instant now = Instant.now();
        int cleaned = 0;
        List<String> issues = new ArrayList<>();
        for (ExportTask task : exportTaskRepository.findExpiredCompletedBefore(now)) {
            try {
                Files.deleteIfExists(buildFilePath(task));
                exportTaskRepository.save(new ExportTask(
                        task.exportId(),
                        task.format(),
                        task.query(),
                        "EXPIRED",
                        task.downloadPath(),
                        task.createdAt(),
                        task.completedAt(),
                        task.expiresAt(),
                        task.recordCount(),
                        task.failureReason()
                ));
                cleaned++;
            } catch (IOException ex) {
                issues.add(ex.getMessage());
            }
        }
        runtimeStateRepository.markCleanup(now,
                issues.isEmpty() ? (cleaned == 0 ? "SKIPPED_EMPTY" : "SUCCESS") : "PARTIAL_SUCCESS",
                issues.isEmpty() ? null : String.join("; ", issues));
        return cleaned;
    }

    public RuntimeOverviewResponse runtimeOverview() {
        RuntimeState state = runtimeStateRepository.snapshot(
                bufferedLogQueueRepository.depth() + retryLogRepository.depth(),
                exportTaskRepository.findByStatus("QUEUED").size(),
                exportTaskRepository.findByStatus("COMPLETED").size(),
                exportTaskRepository.findByStatus("FAILED").size(),
                exportTaskRepository.findByStatus("EXPIRED").size()
        );
        return RuntimeOverviewResponse.from(state);
    }

    public LogSearchResponse internalSearch(InternalLogSearchRequest request) {
        return logQueryService.internalSearch(request);
    }

    public List<AlertRuleResponse> listAlertRules() {
        return alertRuleRepository.findAll().stream().map(AlertRuleResponse::from).toList();
    }

    public List<ExportTaskResponse> listExportTasks() {
        return exportTaskRepository.findAll().stream().map(ExportTaskResponse::from).toList();
    }

    public AlertRuleResponse upsertAlertRule(AlertRuleUpsertRequest request) {
        validateAlertRuleRequest(request);
        AlertRule rule = new AlertRule(
                "RULE-" + UUID.randomUUID(),
                request.ruleName(),
                parseRuleType(request.ruleType()),
                request.serviceName(),
                request.threshold(),
                request.windowMinutes(),
                request.enabled(),
                request.notificationChannels()
        );
        alertRuleRepository.save(rule);
        return AlertRuleResponse.from(rule);
    }

    public void updateAlertRuleEnabled(String ruleId, AlertRuleStatusUpdateRequest request) {
        AlertRule existing = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException("ALERT_RULE_NOT_FOUND", "alert rule not found"));
        alertRuleRepository.save(new AlertRule(
                existing.ruleId(),
                existing.ruleName(),
                existing.ruleType(),
                existing.serviceName(),
                existing.threshold(),
                existing.windowMinutes(),
                request.enabled(),
                existing.notificationChannels()
        ));
    }

    public void deleteAlertRule(String ruleId) {
        if (alertRuleRepository.findById(ruleId).isEmpty()) {
            throw new BusinessException("ALERT_RULE_NOT_FOUND", "alert rule not found");
        }
        alertRuleRepository.delete(ruleId);
    }

    private RuleEvaluationOutcome evaluateRule(AlertRule rule, Instant now) {
        Instant start = now.minus(Duration.ofMinutes(rule.windowMinutes()));
        List<AccessLogRecord> records = logEntryRepository.findWindow(start, now, rule.serviceName());
        if (records.isEmpty()) {
            return RuleEvaluationOutcome.notTriggered();
        }
        long errorCount = records.stream().filter(logQueryService::isErrorRecord).count();
        double total = records.size();
        boolean triggered = false;
        String summary;
        String alertCode;
        switch (rule.ruleType()) {
            case ERROR_RATE -> {
                double errorRate = total == 0 ? 0d : (errorCount * 100d) / total;
                triggered = errorRate >= rule.threshold();
                summary = "error rate %.2f%% exceeded threshold %.2f%%".formatted(errorRate, rule.threshold());
                alertCode = "HIGH_ERROR_RATE";
            }
            case LATENCY_P95 -> {
                double p95 = percentile(records.stream()
                        .map(AccessLogRecord::latencyMs)
                        .filter(latency -> latency != null)
                        .sorted(Comparator.naturalOrder())
                        .toList(), 0.95);
                triggered = p95 >= rule.threshold();
                summary = "p95 latency %.2fms exceeded threshold %.2fms".formatted(p95, rule.threshold());
                alertCode = "HIGH_P95_LATENCY";
            }
            case ERROR_COUNT -> {
                triggered = errorCount >= rule.threshold();
                summary = "error count %d exceeded threshold %.2f".formatted(errorCount, rule.threshold());
                alertCode = "HIGH_ERROR_COUNT";
            }
            default -> throw new BusinessException("VALIDATION_ERROR", "unsupported alert rule type");
        }
        if (!triggered || alertRepository.findOpenByRuleIdAndSummary(rule.ruleId(), summary).isPresent()) {
            return RuleEvaluationOutcome.notTriggered();
        }
        AlertEvent event = new AlertEvent(
                "ALT-" + UUID.randomUUID(),
                alertCode,
                "HIGH",
                "OPEN",
                summary,
                rule.ruleId(),
                now
        );
        alertRepository.save(event);
        String notificationResult = alertNotificationService.notifyChannels(rule, event);
        return new RuleEvaluationOutcome(true, "SUCCESS".equals(notificationResult) ? null : notificationResult);
    }

    private void ensureExportDirectory() {
        try {
            Files.createDirectories(Path.of(properties.export().directory()));
        } catch (IOException ex) {
            throw new BusinessException("INTERNAL_ERROR", ex.getMessage());
        }
    }

    private void ensureFailedBatchDirectory() {
        try {
            Files.createDirectories(Path.of(properties.storage().failedBatchDirectory()));
        } catch (IOException ex) {
            throw new BusinessException("INTERNAL_ERROR", ex.getMessage());
        }
    }

    private Path buildFilePath(ExportTask task) {
        return Path.of(properties.export().directory(), task.exportId() + "." + task.format().toLowerCase(Locale.ROOT));
    }

    private List<AccessLogRecord> selectExportRecords(ExportTask task) {
        if (task.query() == null || task.query().isBlank()) {
            Instant end = Instant.now();
            Instant start = end.minus(Duration.ofMinutes(properties.export().defaultWindowMinutes()));
            return logEntryRepository.findWindow(start, end, null);
        }
        return logEntryRepository.search(new com.example.platform.log.domain.LogSearchCriteria(
                task.query(), null, null, null, null, null, null, 0, properties.export().maxRows() + 1
        ));
    }

    private void writeExport(String format, Path filePath, List<AccessLogRecord> records) throws IOException {
        if ("CSV".equalsIgnoreCase(format)) {
            StringBuilder builder = new StringBuilder("logId,serviceName,traceId,level,message,timestamp\n");
            for (AccessLogRecord record : records) {
                String maskedMessage = logDataMaskingService.maskText(record.message());
                builder.append(record.logId()).append(',')
                        .append(record.serviceName()).append(',')
                        .append(record.traceId()).append(',')
                        .append(record.level()).append(',')
                        .append(sanitizeCsv(maskedMessage)).append(',')
                        .append(record.timestamp()).append('\n');
            }
            Files.writeString(filePath, builder.toString(), StandardCharsets.UTF_8);
            return;
        }
        StringBuilder builder = new StringBuilder("[\n");
        for (int i = 0; i < records.size(); i++) {
            AccessLogRecord record = records.get(i);
            String maskedMessage = logDataMaskingService.maskText(record.message());
            builder.append("  {")
                    .append("\"logId\":\"").append(record.logId()).append("\",")
                    .append("\"serviceName\":\"").append(record.serviceName()).append("\",")
                    .append("\"traceId\":\"").append(record.traceId()).append("\",")
                    .append("\"level\":\"").append(record.level()).append("\",")
                    .append("\"message\":\"").append(escapeJson(maskedMessage)).append("\",")
                    .append("\"timestamp\":\"").append(record.timestamp()).append("\"")
                    .append("}");
            if (i < records.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append(']');
        Files.writeString(filePath, builder.toString(), StandardCharsets.UTF_8);
    }

    private AlertRuleType parseRuleType(String value) {
        try {
            return AlertRuleType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("VALIDATION_ERROR", "unsupported alert rule type");
        }
    }

    private void validateAlertRuleRequest(AlertRuleUpsertRequest request) {
        if (request.notificationChannels() == null) {
            throw new BusinessException("VALIDATION_ERROR", "notificationChannels are required");
        }
        if (request.windowMinutes() <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "windowMinutes must be greater than 0");
        }
        if (request.threshold() <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "threshold must be greater than 0");
        }
    }

    private double percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0d;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private String sanitizeCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void archiveFailedBatch(List<AccessLogRecord> records, String failureReason, Instant timestamp) {
        ensureFailedBatchDirectory();
        Path file = Path.of(
                properties.storage().failedBatchDirectory(),
                "failed-batch-" + timestamp.toEpochMilli() + "-" + UUID.randomUUID() + ".jsonl"
        );
        StringBuilder builder = new StringBuilder();
        for (AccessLogRecord record : records) {
            builder.append("{")
                    .append("\"logId\":\"").append(record.logId()).append("\",")
                    .append("\"serviceName\":\"").append(record.serviceName()).append("\",")
                    .append("\"traceId\":\"").append(record.traceId()).append("\",")
                    .append("\"level\":\"").append(record.level()).append("\",")
                    .append("\"message\":\"").append(escapeJson(record.message())).append("\",")
                    .append("\"timestamp\":\"").append(record.timestamp()).append("\",")
                    .append("\"failureReason\":\"").append(escapeJson(failureReason)).append("\"")
                    .append("}\n");
        }
        try {
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            runtimeStateRepository.markError(ex.getMessage());
        }
    }

    private record RuleEvaluationOutcome(boolean alertCreated, String notificationIssue) {

        private static RuleEvaluationOutcome notTriggered() {
            return new RuleEvaluationOutcome(false, null);
        }
    }
}
