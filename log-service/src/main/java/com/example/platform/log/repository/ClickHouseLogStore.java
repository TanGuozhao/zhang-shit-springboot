package com.example.platform.log.repository;

import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AccessLogRecord;
import com.example.platform.log.domain.LogSearchCriteria;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClickHouseLogStore {

    private final LogServiceProperties properties;

    public ClickHouseLogStore(LogServiceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        if (!properties.storage().clickhouseEnabled()) {
            return;
        }
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS %s (
                        log_id String,
                        service_name String,
                        trace_id String,
                        level String,
                        message String,
                        path String,
                        status_code Nullable(Int32),
                        latency_ms Nullable(Int64),
                        request_id String,
                        client_ip String,
                        timestamp DateTime64(3),
                        tags String
                    ) ENGINE = MergeTree
                    PARTITION BY toYYYYMMDD(timestamp)
                    ORDER BY (service_name, timestamp, trace_id)
                    TTL timestamp + INTERVAL %d DAY
                    """.formatted(properties.storage().clickhouseTable(), properties.storage().retentionDays()));
        } catch (SQLException ignored) {
            // Keep local startup non-blocking; runtime paths will surface the failure when enabled.
        }
    }

    public void saveBatch(List<AccessLogRecord> records) throws SQLException {
        if (!properties.storage().clickhouseEnabled() || records.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO %s
                (log_id, service_name, trace_id, level, message, path, status_code, latency_ms, request_id, client_ip, timestamp, tags)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(properties.storage().clickhouseTable());
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (AccessLogRecord record : records) {
                statement.setString(1, record.logId());
                statement.setString(2, record.serviceName());
                statement.setString(3, record.traceId());
                statement.setString(4, record.level());
                statement.setString(5, record.message());
                statement.setString(6, record.path() == null ? "" : record.path());
                if (record.statusCode() == null) {
                    statement.setObject(7, null);
                } else {
                    statement.setInt(7, record.statusCode());
                }
                if (record.latencyMs() == null) {
                    statement.setObject(8, null);
                } else {
                    statement.setLong(8, record.latencyMs());
                }
                statement.setString(9, record.requestId() == null ? "" : record.requestId());
                statement.setString(10, record.clientIp() == null ? "" : record.clientIp());
                statement.setTimestamp(11, Timestamp.from(record.timestamp()));
                statement.setString(12, flattenTags(record.tags()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<AccessLogRecord> search(LogSearchCriteria criteria) throws SQLException {
        if (!properties.storage().clickhouseEnabled()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT log_id, service_name, trace_id, level, message, path, status_code, latency_ms, request_id, client_ip, timestamp, tags
                FROM %s WHERE 1 = 1
                """.formatted(properties.storage().clickhouseTable()));
        List<Object> parameters = new ArrayList<>();
        appendCriteria(sql, parameters, criteria);
        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        parameters.add(criteria.size());
        parameters.add(criteria.page() * criteria.size());
        return executeSearch(sql.toString(), parameters);
    }

    public long count(LogSearchCriteria criteria) throws SQLException {
        if (!properties.storage().clickhouseEnabled()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("SELECT count() FROM %s WHERE 1 = 1 ".formatted(properties.storage().clickhouseTable()));
        List<Object> parameters = new ArrayList<>();
        appendCriteria(sql, parameters, criteria);
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0;
            }
        }
    }

    public List<AccessLogRecord> findByTraceId(String traceId) throws SQLException {
        if (!properties.storage().clickhouseEnabled()) {
            return List.of();
        }
        String sql = """
                SELECT log_id, service_name, trace_id, level, message, path, status_code, latency_ms, request_id, client_ip, timestamp, tags
                FROM %s WHERE trace_id = ? ORDER BY timestamp ASC
                """.formatted(properties.storage().clickhouseTable());
        return executeSearch(sql, List.of(traceId));
    }

    public List<AccessLogRecord> findWindow(Instant startTime, Instant endTime, String serviceName) throws SQLException {
        if (!properties.storage().clickhouseEnabled()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT log_id, service_name, trace_id, level, message, path, status_code, latency_ms, request_id, client_ip, timestamp, tags
                FROM %s WHERE timestamp >= ? AND timestamp <= ?
                """.formatted(properties.storage().clickhouseTable()));
        List<Object> parameters = new ArrayList<>();
        parameters.add(Timestamp.from(startTime));
        parameters.add(Timestamp.from(endTime));
        if (serviceName != null && !serviceName.isBlank()) {
            sql.append(" AND service_name = ?");
            parameters.add(serviceName);
        }
        sql.append(" ORDER BY timestamp ASC");
        return executeSearch(sql.toString(), parameters);
    }

    private void appendCriteria(StringBuilder sql, List<Object> parameters, LogSearchCriteria criteria) {
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            sql.append(" AND (message LIKE ? OR service_name LIKE ? OR path LIKE ?)");
            String keyword = "%" + criteria.keyword() + "%";
            parameters.add(keyword);
            parameters.add(keyword);
            parameters.add(keyword);
        }
        if (criteria.serviceName() != null && !criteria.serviceName().isBlank()) {
            sql.append(" AND service_name = ?");
            parameters.add(criteria.serviceName());
        }
        if (criteria.level() != null && !criteria.level().isBlank()) {
            sql.append(" AND level = ?");
            parameters.add(criteria.level());
        }
        if (criteria.traceId() != null && !criteria.traceId().isBlank()) {
            sql.append(" AND trace_id = ?");
            parameters.add(criteria.traceId());
        }
        if (criteria.statusCode() != null) {
            sql.append(" AND status_code = ?");
            parameters.add(criteria.statusCode());
        }
        if (criteria.startTime() != null) {
            sql.append(" AND timestamp >= ?");
            parameters.add(Timestamp.from(criteria.startTime()));
        }
        if (criteria.endTime() != null) {
            sql.append(" AND timestamp <= ?");
            parameters.add(Timestamp.from(criteria.endTime()));
        }
    }

    private List<AccessLogRecord> executeSearch(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AccessLogRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(new AccessLogRecord(
                            resultSet.getString("log_id"),
                            resultSet.getString("service_name"),
                            resultSet.getString("trace_id"),
                            resultSet.getString("level"),
                            resultSet.getString("message"),
                            blankToNull(resultSet.getString("path")),
                            (Integer) resultSet.getObject("status_code"),
                            (Long) resultSet.getObject("latency_ms"),
                            blankToNull(resultSet.getString("request_id")),
                            blankToNull(resultSet.getString("client_ip")),
                            resultSet.getTimestamp("timestamp").toInstant(),
                            parseTags(resultSet.getString("tags"))
                    ));
                }
                return records;
            }
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                properties.storage().clickhouseUrl(),
                properties.storage().clickhouseUsername(),
                properties.storage().clickhousePassword()
        );
    }

    private String flattenTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private Map<String, String> parseTags(String raw) {
        Map<String, String> tags = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return tags;
        }
        String[] pairs = raw.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                tags.put(parts[0], parts[1]);
            }
        }
        return tags;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
