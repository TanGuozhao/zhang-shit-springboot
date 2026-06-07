package com.example.platform.message.repository;

import com.example.platform.message.domain.RetryRecord;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RetryRecordRepository {

    private final Map<String, RetryRecord> records = new ConcurrentHashMap<>();

    public void save(RetryRecord record) {
        records.put(record.retryId(), record);
    }

    public Optional<RetryRecord> findById(String retryId) {
        return Optional.ofNullable(records.get(retryId));
    }

    public List<RetryRecord> findAll() {
        return records.values().stream()
                .sorted(Comparator.comparing(RetryRecord::createdAt).reversed())
                .toList();
    }

    public List<RetryRecord> findPending() {
        return findAll().stream()
                .filter(record -> record.retryStatus().equals("PENDING"))
                .toList();
    }

    public List<RetryRecord> findPendingByDispatchTaskId(String dispatchTaskId) {
        return findAll().stream()
                .filter(record -> record.dispatchTaskId().equals(dispatchTaskId))
                .filter(record -> record.retryStatus().equals("PENDING"))
                .toList();
    }

    public List<RetryRecord> findByMessageId(String messageId) {
        return findAll().stream()
                .filter(record -> record.messageId().equals(messageId))
                .toList();
    }

    public void cancelPendingByDispatchTaskId(String dispatchTaskId, String reason) {
        findPendingByDispatchTaskId(dispatchTaskId).forEach(record -> save(new RetryRecord(
                record.retryId(),
                record.messageId(),
                record.dispatchTaskId(),
                record.retryCount(),
                "CANCELLED",
                record.lastRetryAt(),
                reason,
                record.createdAt(),
                java.time.Instant.now()
        )));
    }

    public long countPending() {
        return records.values().stream().filter(record -> record.retryStatus().equals("PENDING")).count();
    }
}
