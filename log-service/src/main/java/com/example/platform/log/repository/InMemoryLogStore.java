package com.example.platform.log.repository;

import com.example.platform.log.domain.AccessLogRecord;
import com.example.platform.log.domain.LogSearchCriteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryLogStore {

    private final CopyOnWriteArrayList<AccessLogRecord> records = new CopyOnWriteArrayList<>();

    public void saveBatch(List<AccessLogRecord> batch) {
        records.addAll(batch);
    }

    public List<AccessLogRecord> search(LogSearchCriteria criteria) {
        return records.stream()
                .filter(record -> matches(record, criteria))
                .sorted(Comparator.comparing(AccessLogRecord::timestamp).reversed())
                .skip((long) criteria.page() * criteria.size())
                .limit(criteria.size())
                .toList();
    }

    public List<AccessLogRecord> findByTraceId(String traceId) {
        return records.stream()
                .filter(record -> record.traceId().equals(traceId))
                .sorted(Comparator.comparing(AccessLogRecord::timestamp))
                .toList();
    }

    public List<AccessLogRecord> findWindow(Instant startTime, Instant endTime, String serviceName) {
        return records.stream()
                .filter(record -> serviceName == null || serviceName.isBlank() || serviceName.equals(record.serviceName()))
                .filter(record -> !record.timestamp().isBefore(startTime) && !record.timestamp().isAfter(endTime))
                .sorted(Comparator.comparing(AccessLogRecord::timestamp))
                .toList();
    }

    public long count(LogSearchCriteria criteria) {
        return records.stream().filter(record -> matches(record, criteria)).count();
    }

    public List<AccessLogRecord> all() {
        return new ArrayList<>(records);
    }

    private boolean matches(AccessLogRecord record, LogSearchCriteria criteria) {
        boolean keywordMatch = criteria.keyword() == null || criteria.keyword().isBlank()
                || record.message().contains(criteria.keyword())
                || record.serviceName().contains(criteria.keyword())
                || (record.path() != null && record.path().contains(criteria.keyword()));
        boolean serviceMatch = criteria.serviceName() == null || criteria.serviceName().isBlank()
                || criteria.serviceName().equals(record.serviceName());
        boolean levelMatch = criteria.level() == null || criteria.level().isBlank()
                || criteria.level().equalsIgnoreCase(record.level());
        boolean traceMatch = criteria.traceId() == null || criteria.traceId().isBlank()
                || criteria.traceId().equals(record.traceId());
        boolean statusMatch = criteria.statusCode() == null
                || (record.statusCode() != null && criteria.statusCode().equals(record.statusCode()));
        boolean startMatch = criteria.startTime() == null || !record.timestamp().isBefore(criteria.startTime());
        boolean endMatch = criteria.endTime() == null || !record.timestamp().isAfter(criteria.endTime());
        return keywordMatch && serviceMatch && levelMatch && traceMatch && statusMatch && startMatch && endMatch;
    }
}
