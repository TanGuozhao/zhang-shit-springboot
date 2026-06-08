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
import com.example.platform.log.repository.BufferedLogQueueRepository;
import com.example.platform.log.repository.ExportTaskRepository;
import com.example.platform.log.repository.LogEntryRepository;
import com.example.platform.log.repository.RuntimeStateRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private final ExportTaskRepository exportTaskRepository;
    private final BufferedLogQueueRepository bufferedLogQueueRepository;
    private final RuntimeStateRepository runtimeStateRepository;
    private final LogServiceProperties properties;
    private final Set<String> allowedLevels;

    public LogQueryService(LogEntryRepository logEntryRepository,
                           AlertRepository alertRepository,
                           ExportTaskRepository exportTaskRepository,
                           BufferedLogQueueRepository bufferedLogQueueRepository,
                           RuntimeStateRepository runtimeStateRepository,
                           LogServiceProperties properties) {
        this.logEntryRepository = logEntryRepository;
        this.alertRepository = alertRepository;
        this.exportTaskRepository = exportTaskRepository;
        this.bufferedLogQueueRepository = bufferedLogQueueRepository;
        this.runtimeStateRepository = runtimeStateRepository;
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
        long total = records.size();
        long errorCount = records.stream().filter(this::isErrorRecord).count();
        long successCount = records.stream().filter(this::isSuccessRecord).count();
        long http4xxCount = records.stream().filter(this::is4xxRecord).count();
        long http5xxCount = records.stream().filter(this::is5xxRecord).count();
        long timeoutCount = records.stream().filter(this::isTimeoutRecord).count();
        long errorLogCount = records.stream().filter(this::isErrorLevelRecord).count();
        long bizThroughput = records.stream().filter(this::isBusinessRequest).count();
        long uniqueTraceCount = records.stream()
                .map(AccessLogRecord::traceId)
                .filter(traceId -> traceId != null && !traceId.isBlank())
                .distinct()
                .count();
        long uniqueClientIpCount = records.stream()
                .map(AccessLogRecord::clientIp)
                .filter(clientIp -> clientIp != null && !clientIp.isBlank())
                .distinct()
                .count();

        List<Long> latencies = sortedLatencies(records);
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0d);
        long maxLatency = latencies.isEmpty() ? 0L : latencies.get(latencies.size() - 1);
        long minLatency = latencies.isEmpty() ? 0L : latencies.get(0);
        double p95 = percentile(latencies, 0.95);

        Instant minTimestamp = records.stream().map(AccessLogRecord::timestamp).min(Comparator.naturalOrder()).orElse(start);
        Instant maxTimestamp = records.stream().map(AccessLogRecord::timestamp).max(Comparator.naturalOrder()).orElse(end);
        double windowSeconds = Math.max(1d, Duration.between(minTimestamp, maxTimestamp).toMillis() / 1000d);

        long previousTotal = countPreviousWindow(end, start, serviceName);
        double momGrowth = previousTotal == 0
                ? (total == 0 ? 0d : 100d)
                : ((total - previousTotal) * 100d) / previousTotal;
        double tps = successCount / windowSeconds;

        long alertTriggerCount = alertRepository.findAll().stream()
                .filter(alert -> !alert.createdAt().isBefore(start))
                .count();
        long alertUnresolvedCount = alertRepository.findAll().stream()
                .filter(alert -> "OPEN".equalsIgnoreCase(alert.status()) || "PROCESSING".equalsIgnoreCase(alert.status()))
                .count();

        long pendingExports = exportTaskRepository.findByStatus("QUEUED").size();
        long completedExports = exportTaskRepository.findByStatus("COMPLETED").size();
        long failedExports = exportTaskRepository.findByStatus("FAILED").size();
        long expiredExports = exportTaskRepository.findByStatus("EXPIRED").size();

        int queueDepth = bufferedLogQueueRepository.depth();
        double bufferUsageRate = bufferedLogQueueRepository.capacity() == 0
                ? 0d
                : (queueDepth * 100d) / bufferedLogQueueRepository.capacity();
        int archivedFailedBatches = runtimeStateRepository.snapshot(
                queueDepth,
                (int) pendingExports,
                (int) completedExports,
                (int) failedExports,
                (int) expiredExports
        ).archivedFailedBatches();

        metrics.put("qps", round(total / windowSeconds));
        metrics.put("tps", round(tps));
        metrics.put("totalRequests", total);
        metrics.put("bizThroughput", bizThroughput);
        metrics.put("successRate", round(total == 0 ? 0d : (successCount * 100d) / total));
        metrics.put("errorRate", round(total == 0 ? 0d : (errorCount * 100d) / total));
        metrics.put("http4xxCount", http4xxCount);
        metrics.put("http5xxCount", http5xxCount);
        metrics.put("timeoutCount", timeoutCount);
        metrics.put("errorLogCount", errorLogCount);
        metrics.put("avgLatency", round(avgLatency));
        metrics.put("maxLatency", maxLatency);
        metrics.put("minLatency", minLatency);
        metrics.put("p95", round(p95));
        metrics.put("p95Latency", round(p95));
        metrics.put("momGrowth", round(momGrowth));
        metrics.put("uniqueTraceCount", uniqueTraceCount);
        metrics.put("uniqueClientIpCount", uniqueClientIpCount);
        metrics.put("alertTriggerCount", alertTriggerCount);
        metrics.put("alertUnresolvedCount", alertUnresolvedCount);
        metrics.put("pendingExports", pendingExports);
        metrics.put("completedExports", completedExports);
        metrics.put("failedExports", failedExports);
        metrics.put("expiredExports", expiredExports);
        metrics.put("bufferDepth", queueDepth);
        metrics.put("bufferUsageRate", round(bufferUsageRate));
        metrics.put("archivedFailedBatches", archivedFailedBatches);
        return new MetricsResponse(serviceName, metrics);
    }

    public List<AlertResponse> alerts() {
        return alertRepository.findAll().stream().map(AlertResponse::from).toList();
    }

    public boolean isErrorRecord(AccessLogRecord record) {
        return isErrorLevelRecord(record) || is5xxRecord(record);
    }

    private boolean isSuccessRecord(AccessLogRecord record) {
        return record.statusCode() != null && record.statusCode() >= 200 && record.statusCode() < 300;
    }

    private boolean is4xxRecord(AccessLogRecord record) {
        return record.statusCode() != null && record.statusCode() >= 400 && record.statusCode() < 500;
    }

    private boolean is5xxRecord(AccessLogRecord record) {
        return record.statusCode() != null && record.statusCode() >= 500;
    }

    private boolean isTimeoutRecord(AccessLogRecord record) {
        return record.latencyMs() != null && record.latencyMs() > 3000;
    }

    private boolean isErrorLevelRecord(AccessLogRecord record) {
        return "ERROR".equalsIgnoreCase(record.level()) || "FATAL".equalsIgnoreCase(record.level());
    }

    private boolean isBusinessRequest(AccessLogRecord record) {
        String path = record.path();
        if (path == null || path.isBlank()) {
            return false;
        }
        return !path.startsWith("/actuator")
                && !path.startsWith("/health")
                && !path.startsWith("/internal")
                && !path.endsWith(".js")
                && !path.endsWith(".css")
                && !path.endsWith(".png")
                && !path.endsWith(".ico");
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

    private List<Long> sortedLatencies(List<AccessLogRecord> records) {
        List<Long> latencies = new ArrayList<>();
        for (AccessLogRecord record : records) {
            if (record.latencyMs() != null) {
                latencies.add(record.latencyMs());
            }
        }
        latencies.sort(Comparator.naturalOrder());
        return latencies;
    }

    private long countPreviousWindow(Instant end, Instant start, String serviceName) {
        Duration window = Duration.between(start, end);
        if (window.isNegative() || window.isZero()) {
            return 0L;
        }
        Instant previousEnd = start;
        Instant previousStart = previousEnd.minus(window);
        return logEntryRepository.findWindow(previousStart, previousEnd, serviceName).size();
    }
}
