package com.example.platform.log.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.log.domain.AlertEvent;
import com.example.platform.log.domain.LogEntry;
import com.example.platform.log.dto.AlertStatusUpdateRequest;
import com.example.platform.log.dto.ExportRequest;
import com.example.platform.log.dto.ExportResponse;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.log.repository.AlertRepository;
import com.example.platform.log.repository.LogEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LogCommandService {

    private final LogEntryRepository logEntryRepository;
    private final AlertRepository alertRepository;

    public LogCommandService(LogEntryRepository logEntryRepository, AlertRepository alertRepository) {
        this.logEntryRepository = logEntryRepository;
        this.alertRepository = alertRepository;
    }

    public LogEntryResponse ingest(LogIngestRequest request) {
        LogEntry entry = new LogEntry(
                "LOG-" + UUID.randomUUID(),
                request.serviceName(),
                request.traceId(),
                request.level(),
                request.message(),
                Instant.now()
        );
        logEntryRepository.save(entry);
        return new LogEntryResponse(
                entry.logId(),
                entry.serviceName(),
                entry.traceId(),
                entry.level(),
                entry.message(),
                entry.timestamp()
        );
    }

    public void updateAlertStatus(String alertId, AlertStatusUpdateRequest request) {
        AlertEvent alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("ALERT_NOT_FOUND", "alert not found"));
        alertRepository.save(new AlertEvent(
                alert.alertId(),
                alert.alertCode(),
                alert.level(),
                request.status(),
                alert.summary()
        ));
    }

    public ExportResponse createExport(ExportRequest request) {
        String exportId = "EXP-" + UUID.randomUUID();
        return new ExportResponse(exportId, "QUEUED", "/downloads/" + exportId + "." + request.format().toLowerCase());
    }
}
