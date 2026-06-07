package com.example.platform.log.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AccessLogRecord;
import com.example.platform.log.domain.AlertEvent;
import com.example.platform.log.domain.ExportTask;
import com.example.platform.log.dto.AlertStatusUpdateRequest;
import com.example.platform.log.dto.ExportRequest;
import com.example.platform.log.dto.ExportResponse;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.log.repository.AlertRepository;
import com.example.platform.log.repository.BufferedLogQueueRepository;
import com.example.platform.log.repository.ExportTaskRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LogCommandService {

    private final LogServiceProperties properties;
    private final BufferedLogQueueRepository bufferedLogQueueRepository;
    private final AlertRepository alertRepository;
    private final ExportTaskRepository exportTaskRepository;
    private final Set<String> allowedLevels;

    public LogCommandService(LogServiceProperties properties,
                             BufferedLogQueueRepository bufferedLogQueueRepository,
                             AlertRepository alertRepository,
                             ExportTaskRepository exportTaskRepository) {
        this.properties = properties;
        this.bufferedLogQueueRepository = bufferedLogQueueRepository;
        this.alertRepository = alertRepository;
        this.exportTaskRepository = exportTaskRepository;
        this.allowedLevels = new HashSet<>(properties.ingest().allowedLevels());
    }

    public LogEntryResponse ingest(LogIngestRequest request) {
        validateIngestRequest(request);
        AccessLogRecord record = new AccessLogRecord(
                "LOG-" + UUID.randomUUID(),
                request.serviceName(),
                request.traceId(),
                request.level().toUpperCase(Locale.ROOT),
                request.message(),
                request.path(),
                request.statusCode(),
                request.latencyMs(),
                request.requestId(),
                request.clientIp(),
                request.timestamp() == null ? Instant.now() : request.timestamp(),
                request.tags()
        );
        if (!bufferedLogQueueRepository.offer(record)) {
            throw new BusinessException("QUEUE_FULL", "log buffer queue is full");
        }
        return LogEntryResponse.from(record);
    }

    public void updateAlertStatus(String alertId, AlertStatusUpdateRequest request) {
        AlertEvent alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("ALERT_NOT_FOUND", "alert not found"));
        alertRepository.save(new AlertEvent(
                alert.alertId(),
                alert.alertCode(),
                alert.level(),
                request.status(),
                alert.summary(),
                alert.ruleId(),
                alert.createdAt()
        ));
    }

    public ExportResponse createExport(ExportRequest request) {
        validateExportRequest(request);
        String format = request.format().toUpperCase(Locale.ROOT);
        if (!format.equals("CSV") && !format.equals("JSON")) {
            throw new BusinessException("UNSUPPORTED_EXPORT_FORMAT", "unsupported export format");
        }
        String exportId = "EXP-" + UUID.randomUUID();
        String downloadPath = "/downloads/" + exportId + "." + format.toLowerCase(Locale.ROOT);
        exportTaskRepository.save(new ExportTask(
                exportId,
                format,
                request.query() == null ? "" : request.query(),
                "QUEUED",
                downloadPath,
                Instant.now(),
                null,
                null,
                null,
                null
        ));
        return new ExportResponse(exportId, "QUEUED", downloadPath);
    }

    private void validateIngestRequest(LogIngestRequest request) {
        String normalizedLevel = request.level().toUpperCase(Locale.ROOT);
        if (!allowedLevels.contains(normalizedLevel)) {
            throw new BusinessException("INVALID_LOG_LEVEL", "unsupported log level");
        }
        if (request.statusCode() != null && request.statusCode() > 599) {
            throw new BusinessException("INVALID_STATUS_CODE", "statusCode must be between 0 and 599");
        }
        if (request.message().length() > properties.ingest().maxMessageLength()) {
            throw new BusinessException("MESSAGE_TOO_LONG", "message length exceeds maximum limit");
        }
        Instant now = Instant.now();
        Instant timestamp = request.timestamp() == null ? now : request.timestamp();
        if (timestamp.isAfter(now.plusSeconds(properties.ingest().maxFutureSkewSeconds()))) {
            throw new BusinessException("TIMESTAMP_OUT_OF_RANGE", "timestamp is too far in the future");
        }
        if (timestamp.isBefore(now.minus(Duration.ofDays(properties.ingest().maxPastDays())))) {
            throw new BusinessException("TIMESTAMP_OUT_OF_RANGE", "timestamp is older than the allowed retention window");
        }
    }

    private void validateExportRequest(ExportRequest request) {
        if (request.query() != null && request.query().length() > properties.search().maxKeywordLength()) {
            throw new BusinessException("KEYWORD_TOO_LONG", "query length exceeds maximum limit");
        }
    }
}
