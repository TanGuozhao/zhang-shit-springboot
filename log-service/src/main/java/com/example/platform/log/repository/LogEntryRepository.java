package com.example.platform.log.repository;

import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AccessLogRecord;
import com.example.platform.log.domain.LogSearchCriteria;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class LogEntryRepository {

    private final InMemoryLogStore inMemoryLogStore;
    private final ClickHouseLogStore clickHouseLogStore;
    private final LogServiceProperties properties;

    public LogEntryRepository(InMemoryLogStore inMemoryLogStore,
                              ClickHouseLogStore clickHouseLogStore,
                              LogServiceProperties properties) {
        this.inMemoryLogStore = inMemoryLogStore;
        this.clickHouseLogStore = clickHouseLogStore;
        this.properties = properties;
    }

    public void saveBatch(List<AccessLogRecord> records) throws SQLException {
        if (records.isEmpty()) {
            return;
        }
        if (properties.storage().clickhouseEnabled()) {
            clickHouseLogStore.saveBatch(records);
        }
        inMemoryLogStore.saveBatch(records);
    }

    public List<AccessLogRecord> search(LogSearchCriteria criteria) {
        if (properties.storage().clickhouseEnabled()) {
            try {
                return clickHouseLogStore.search(criteria);
            } catch (SQLException ignored) {
                return inMemoryLogStore.search(criteria);
            }
        }
        return inMemoryLogStore.search(criteria);
    }

    public long count(LogSearchCriteria criteria) {
        if (properties.storage().clickhouseEnabled()) {
            try {
                return clickHouseLogStore.count(criteria);
            } catch (SQLException ignored) {
                return inMemoryLogStore.count(criteria);
            }
        }
        return inMemoryLogStore.count(criteria);
    }

    public List<AccessLogRecord> findByTraceId(String traceId) {
        if (properties.storage().clickhouseEnabled()) {
            try {
                return clickHouseLogStore.findByTraceId(traceId);
            } catch (SQLException ignored) {
                return inMemoryLogStore.findByTraceId(traceId);
            }
        }
        return inMemoryLogStore.findByTraceId(traceId);
    }

    public List<AccessLogRecord> findWindow(Instant startTime, Instant endTime, String serviceName) {
        if (properties.storage().clickhouseEnabled()) {
            try {
                return clickHouseLogStore.findWindow(startTime, endTime, serviceName);
            } catch (SQLException ignored) {
                return inMemoryLogStore.findWindow(startTime, endTime, serviceName);
            }
        }
        return inMemoryLogStore.findWindow(startTime, endTime, serviceName);
    }

    public List<AccessLogRecord> all() {
        return inMemoryLogStore.all();
    }
}
