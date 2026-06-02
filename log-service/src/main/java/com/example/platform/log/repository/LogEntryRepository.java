package com.example.platform.log.repository;

import com.example.platform.log.domain.LogEntry;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class LogEntryRepository {

    private final List<LogEntry> entries = new ArrayList<>();

    public LogEntryRepository() {
        entries.add(new LogEntry("LOG-SEED-1", "topbiz", "TRACE-1001", "INFO", "topbiz started", Instant.now()));
        entries.add(new LogEntry("LOG-SEED-2", "message-service", "TRACE-1001", "WARN", "message retry scheduled", Instant.now()));
    }

    public void save(LogEntry entry) {
        entries.add(entry);
    }

    public List<LogEntry> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.copyOf(entries);
        }
        return entries.stream()
                .filter(entry -> entry.message().contains(keyword) || entry.serviceName().contains(keyword))
                .toList();
    }

    public List<LogEntry> findByTraceId(String traceId) {
        return entries.stream().filter(entry -> entry.traceId().equals(traceId)).toList();
    }
}
