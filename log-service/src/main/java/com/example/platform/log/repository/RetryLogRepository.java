package com.example.platform.log.repository;

import com.example.platform.log.domain.AccessLogRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class RetryLogRepository {

    private final CopyOnWriteArrayList<AccessLogRecord> failedRecords = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> retryAttempts = new ConcurrentHashMap<>();

    public List<AccessLogRecord> drain(int maxSize) {
        List<AccessLogRecord> snapshot = new ArrayList<>();
        int count = Math.min(maxSize, failedRecords.size());
        for (int i = 0; i < count; i++) {
            snapshot.add(failedRecords.get(i));
        }
        failedRecords.removeAll(snapshot);
        return snapshot;
    }

    public void stashAll(List<AccessLogRecord> records) {
        for (AccessLogRecord record : records) {
            retryAttempts.merge(record.logId(), 1, Integer::sum);
            if (failedRecords.stream().noneMatch(existing -> existing.logId().equals(record.logId()))) {
                failedRecords.add(record);
            }
        }
    }

    public List<AccessLogRecord> findExhausted(int maxAttempts) {
        return failedRecords.stream()
                .filter(record -> retryAttempts.getOrDefault(record.logId(), 0) >= maxAttempts)
                .toList();
    }

    public void markSucceeded(List<AccessLogRecord> records) {
        discard(records);
    }

    public void discard(List<AccessLogRecord> records) {
        List<String> ids = records.stream().map(AccessLogRecord::logId).toList();
        failedRecords.removeIf(record -> ids.contains(record.logId()));
        ids.forEach(retryAttempts::remove);
    }

    public int depth() {
        return failedRecords.size();
    }
}
