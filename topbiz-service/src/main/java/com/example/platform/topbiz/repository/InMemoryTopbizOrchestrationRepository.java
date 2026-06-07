package com.example.platform.topbiz.repository;

import com.example.platform.topbiz.domain.OrchestrationExecutionRecord;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryTopbizOrchestrationRepository implements TopbizOrchestrationRepository {

    private final ConcurrentMap<String, OrchestrationExecutionRecord> records = new ConcurrentHashMap<>();

    @Override
    public OrchestrationExecutionRecord save(OrchestrationExecutionRecord record) {
        records.put(record.orchestrationId(), record);
        return record;
    }

    @Override
    public Optional<OrchestrationExecutionRecord> findById(String orchestrationId) {
        return Optional.ofNullable(records.get(orchestrationId));
    }

    @Override
    public List<OrchestrationExecutionRecord> findAll(String orchestrationType, String status, Integer limit) {
        return records.values().stream()
                .filter(record -> orchestrationType == null || orchestrationType.isBlank()
                        || record.orchestrationType().equalsIgnoreCase(orchestrationType.trim()))
                .filter(record -> status == null || status.isBlank()
                        || record.status().equalsIgnoreCase(status.trim()))
                .sorted(Comparator.comparing(OrchestrationExecutionRecord::startedAt).reversed())
                .limit(resolveLimit(limit))
                .toList();
    }

    private long resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50L;
        }
        return limit.longValue();
    }
}
