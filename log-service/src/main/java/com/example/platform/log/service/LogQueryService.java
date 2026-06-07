package com.example.platform.log.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AccessLogRecord;
import com.example.platform.log.domain.LogSearchCriteria;
import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.InternalLogSearchRequest;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.log.repository.AlertRepository;
import com.example.platform.log.repository.LogEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LogQueryService {

    private final LogEntryRepository logEntryRepository;
    private final AlertRepository alertRepository;
    private final LogServiceProperties properties;
    private final Set<String> allowedLevels;

    public LogQueryService(LogEntryRepository logEntryRepository,
                           AlertRepository alertRepository,
                           LogServiceProperties properties) {
        this.logEntryRepository = logEntryRepository;
        this.alertRepository = alertRepository;
        this.properties = properties;
        this.allowedLevels = new HashSet<>(properties.ingest().allowedLevels());
    }

    public LogSearchResponse search(String keyword) {
        validateKeyword(keyword);
        LogSearchCriteria criteria = LogSearchCriteria.keywordOnly(keyword);
        return toSearchResponse(criteria, logEntryRepository.search(criteria), logEntryRepository.count(criteria));
    }

    public LogSearchResponse internalSearch(InternalLogSearchRequest request) {
        validateInternalSearch(request);
        Instant endTime = request.endTime() == null ? Instant.now() : request.endTime();
        Instant startTime = request.startTime() == null
                ? endTime.minus(Duration.ofHours(properties.search().defaultWindowHours()))
                : request.startTime();
        LogSearchCriteria criteria = new LogSearchCriteria(
                request.keyword(),
                request.serviceName(),
                request.level(),
                request.traceId(),
                request.statusCode(),
                startTime,
                endTime,
                request.page() == null ? 0 : request.page(),
                request.size() == null ? 20 : request.size()
        );
        return toSearchResponse(criteria, logEntryRepository.search(criteria), logEntryRepository.count(criteria));
    }

    public LogSearchResponse trace(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "traceId is required");
        }
        List<AccessLogRecord> records = logEntryRepository.findByTraceId(traceId);
        List<LogEntryResponse> responses = records.stream().map(LogEntryResponse::from).toList();
        return new LogSearchResponse(responses, responses.size());
    }

    public MetricsResponse metrics(String serviceName) {
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofHours(1));
        List<AccessLogRecord> records = logEntryRepository.findWindow(start, end, serviceName);
        Map<String, Number> metrics = new LinkedHashMap<>();
        if (records.isEmpty()) {
            metrics.put("qps", 0d);
            metrics.put("errorRate", 0d);
            metrics.put("avgLatency", 0d);
            metrics.put("p95", 0d);
            metrics.put("p95Latency", 0d);
            metrics.put("totalRequests", 0);
            return new MetricsResponse(serviceName, metrics);
        }

        long total = records.size();
        long errorCount = records.stream().filter(this::isErrorRecord).count();
        double avgLatency = records.stream()
                .map(AccessLogRecord::latencyMs)
                .filter(latency -> latency != null)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0d);
        List<Long> latencies = records.stream()
                .map(AccessLogRecord::latencyMs)
                .filter(latency -> latency != null)
                .sorted(Comparator.naturalOrder())
                .toList();
        double p95 = percentile(latencies, 0.95);
        Instant minTimestamp = records.stream().map(AccessLogRecord::timestamp).min(Comparator.naturalOrder()).orElse(start);
        Instant maxTimestamp = records.stream().map(AccessLogRecord::timestamp).max(Comparator.naturalOrder()).orElse(end);
        double windowSeconds = Math.max(1d, Duration.between(minTimestamp, maxTimestamp).toMillis() / 1000d);

        metrics.put("qps", round(total / windowSeconds));
        metrics.put("errorRate", round(total == 0 ? 0d : (errorCount * 100d) / total));
        metrics.put("avgLatency", round(avgLatency));
        metrics.put("p95", round(p95));
        metrics.put("p95Latency", round(p95));
        metrics.put("totalRequests", total);
        return new MetricsResponse(serviceName, metrics);
    }

    public List<AlertResponse> alerts() {
        return alertRepository.findAll().stream().map(AlertResponse::from).toList();
    }

    public boolean isErrorRecord(AccessLogRecord record) {
        return "ERROR".equalsIgnoreCase(record.level())
                || "FATAL".equalsIgnoreCase(record.level())
                || (record.statusCode() != null && record.statusCode() >= 500);
    }

    private void validateInternalSearch(InternalLogSearchRequest request) {
        validateKeyword(request.keyword());
        if (request.level() != null && !request.level().isBlank() && !allowedLevels.contains(request.level().toUpperCase())) {
            throw new BusinessException("INVALID_LOG_LEVEL", "unsupported log level");
        }
        if (request.statusCode() != null && (request.statusCode() < 0 || request.statusCode() > 599)) {
            throw new BusinessException("INVALID_STATUS_CODE", "statusCode must be between 0 and 599");
        }
        int size = request.size() == null ? 20 : request.size();
        if (size > properties.search().maxPageSize()) {
            throw new BusinessException("PAGE_SIZE_EXCEEDED", "size exceeds maximum page size");
        }
        Instant endTime = request.endTime() == null ? Instant.now() : request.endTime();
        Instant startTime = request.startTime() == null
                ? endTime.minus(Duration.ofHours(properties.search().defaultWindowHours()))
                : request.startTime();
        if (endTime.isBefore(startTime)) {
            throw new BusinessException("INVALID_TIME_RANGE", "endTime must be greater than or equal to startTime");
        }
        long maxWindowDays = properties.search().maxWindowDays();
        if (Duration.between(startTime, endTime).compareTo(Duration.ofDays(maxWindowDays)) > 0) {
            throw new BusinessException("SEARCH_WINDOW_EXCEEDED", "time range exceeds maximum window of %d days".formatted(maxWindowDays));
        }
    }

    private void validateKeyword(String keyword) {
        if (keyword != null && keyword.length() > properties.search().maxKeywordLength()) {
            throw new BusinessException("KEYWORD_TOO_LONG", "keyword length exceeds maximum limit");
        }
    }

    private LogSearchResponse toSearchResponse(LogSearchCriteria criteria,
                                               List<AccessLogRecord> records,
                                               long total) {
        List<LogEntryResponse> responses = records.stream().map(LogEntryResponse::from).toList();
        return new LogSearchResponse(responses, total == 0 ? responses.size() : total);
    }

    private double percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0d;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
