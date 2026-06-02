package com.example.platform.log.service;

import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.log.repository.AlertRepository;
import com.example.platform.log.repository.LogEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LogQueryService {

    private final LogEntryRepository logEntryRepository;
    private final AlertRepository alertRepository;

    public LogQueryService(LogEntryRepository logEntryRepository, AlertRepository alertRepository) {
        this.logEntryRepository = logEntryRepository;
        this.alertRepository = alertRepository;
    }

    public LogSearchResponse search(String keyword) {
        List<LogEntryResponse> records = logEntryRepository.search(keyword)
                .stream()
                .map(entry -> new LogEntryResponse(
                        entry.logId(),
                        entry.serviceName(),
                        entry.traceId(),
                        entry.level(),
                        entry.message(),
                        entry.timestamp()
                ))
                .toList();
        return new LogSearchResponse(records, records.size());
    }

    public LogSearchResponse trace(String traceId) {
        List<LogEntryResponse> records = logEntryRepository.findByTraceId(traceId)
                .stream()
                .map(entry -> new LogEntryResponse(
                        entry.logId(),
                        entry.serviceName(),
                        entry.traceId(),
                        entry.level(),
                        entry.message(),
                        entry.timestamp()
                ))
                .toList();
        return new LogSearchResponse(records, records.size());
    }

    public MetricsResponse metrics(String serviceName) {
        return new MetricsResponse(serviceName, Map.of("qps", 128, "errorRate", 1.2, "p95", 240));
    }

    public List<AlertResponse> alerts() {
        return alertRepository.findAll().stream()
                .map(alert -> new AlertResponse(
                        alert.alertId(),
                        alert.alertCode(),
                        alert.level(),
                        alert.status(),
                        alert.summary()
                ))
                .toList();
    }
}
