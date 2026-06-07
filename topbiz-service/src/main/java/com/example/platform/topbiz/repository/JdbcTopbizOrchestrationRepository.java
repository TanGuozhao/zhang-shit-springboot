package com.example.platform.topbiz.repository;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.config.TopbizOrchestrationPersistenceProperties;
import com.example.platform.topbiz.domain.OrchestrationExecutionRecord;
import com.example.platform.topbiz.domain.OrchestrationStepRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcTopbizOrchestrationRepository implements TopbizOrchestrationRepository {

    private static final TypeReference<List<OrchestrationStepRecord>> STEP_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> RESULT_MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String tableName;
    private final RowMapper<OrchestrationExecutionRecord> rowMapper = (rs, rowNum) -> mapRecord(rs);

    public JdbcTopbizOrchestrationRepository(JdbcTemplate jdbcTemplate,
                                             ObjectMapper objectMapper,
                                             TopbizOrchestrationPersistenceProperties.Jdbc jdbcProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.tableName = jdbcProperties.getTableName();
        if (jdbcProperties.isInitializeSchema()) {
            initializeSchema();
        }
    }

    @Override
    public OrchestrationExecutionRecord save(OrchestrationExecutionRecord record) {
        String stepsJson = writeJson(record.steps());
        String resultJson = writeJson(record.result());
        int updated = jdbcTemplate.update("""
                        UPDATE %s
                           SET orchestration_type = ?,
                               business_key = ?,
                               status = ?,
                               trace_id = ?,
                               started_at = ?,
                               finished_at = ?,
                               steps_json = ?,
                               result_json = ?,
                               error_code = ?,
                               error_message = ?
                         WHERE orchestration_id = ?
                        """.formatted(tableName),
                record.orchestrationType(),
                record.businessKey(),
                record.status(),
                record.traceId(),
                Timestamp.from(record.startedAt()),
                Timestamp.from(record.finishedAt()),
                stepsJson,
                resultJson,
                record.errorCode(),
                record.errorMessage(),
                record.orchestrationId()
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            INSERT INTO %s (
                                orchestration_id,
                                orchestration_type,
                                business_key,
                                status,
                                trace_id,
                                started_at,
                                finished_at,
                                steps_json,
                                result_json,
                                error_code,
                                error_message
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """.formatted(tableName),
                    record.orchestrationId(),
                    record.orchestrationType(),
                    record.businessKey(),
                    record.status(),
                    record.traceId(),
                    Timestamp.from(record.startedAt()),
                    Timestamp.from(record.finishedAt()),
                    stepsJson,
                    resultJson,
                    record.errorCode(),
                    record.errorMessage()
            );
        }
        return record;
    }

    @Override
    public Optional<OrchestrationExecutionRecord> findById(String orchestrationId) {
        List<OrchestrationExecutionRecord> results = jdbcTemplate.query("""
                        SELECT orchestration_id,
                               orchestration_type,
                               business_key,
                               status,
                               trace_id,
                               started_at,
                               finished_at,
                               steps_json,
                               result_json,
                               error_code,
                               error_message
                          FROM %s
                         WHERE orchestration_id = ?
                        """.formatted(tableName),
                rowMapper,
                orchestrationId
        );
        return results.stream().findFirst();
    }

    @Override
    public List<OrchestrationExecutionRecord> findAll(String orchestrationType, String status, Integer limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT orchestration_id,
                       orchestration_type,
                       business_key,
                       status,
                       trace_id,
                       started_at,
                       finished_at,
                       steps_json,
                       result_json,
                       error_code,
                       error_message
                  FROM %s
                """.formatted(tableName));
        List<Object> arguments = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (StringUtils.hasText(orchestrationType)) {
            conditions.add("UPPER(orchestration_type) = ?");
            arguments.add(orchestrationType.trim().toUpperCase());
        }
        if (StringUtils.hasText(status)) {
            conditions.add("UPPER(status) = ?");
            arguments.add(status.trim().toUpperCase());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY started_at DESC LIMIT ?");
        arguments.add(resolveLimit(limit));
        return jdbcTemplate.query(sql.toString(), rowMapper, arguments.toArray());
    }

    private void initializeSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    orchestration_id VARCHAR(96) PRIMARY KEY,
                    orchestration_type VARCHAR(64) NOT NULL,
                    business_key VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    trace_id VARCHAR(128) NOT NULL,
                    started_at TIMESTAMP(6) NOT NULL,
                    finished_at TIMESTAMP(6) NOT NULL,
                    steps_json TEXT NOT NULL,
                    result_json TEXT NOT NULL,
                    error_code VARCHAR(64),
                    error_message TEXT
                )
                """.formatted(tableName));
        createIndexIfMissing(indexName("type_started_at"), "orchestration_type, started_at");
        createIndexIfMissing(indexName("status_started_at"), "status, started_at");
    }

    private void createIndexIfMissing(String indexName, String columnClause) {
        if (indexExists(indexName)) {
            return;
        }
        jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + " (" + columnClause + ")");
    }

    private boolean indexExists(String indexName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet indexResult = indexResult(connection.getMetaData())) {
            while (indexResult.next()) {
                String currentIndex = indexResult.getString("INDEX_NAME");
                if (currentIndex != null && currentIndex.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            throw new BusinessException(
                    "ORCHESTRATION_SCHEMA_INIT_FAILED",
                    "failed to inspect orchestration repository indexes",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex
            );
        }
    }

    private ResultSet indexResult(DatabaseMetaData metadata) throws SQLException {
        return metadata.getIndexInfo(null, null, tableName, false, false);
    }

    private String indexName(String suffix) {
        return "idx_" + tableName + "_" + suffix;
    }

    private OrchestrationExecutionRecord mapRecord(ResultSet rs) throws SQLException {
        return new OrchestrationExecutionRecord(
                rs.getString("orchestration_id"),
                rs.getString("orchestration_type"),
                rs.getString("business_key"),
                rs.getString("status"),
                rs.getString("trace_id"),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("finished_at")),
                readJson(rs.getString("steps_json"), STEP_LIST_TYPE),
                readJson(rs.getString("result_json"), RESULT_MAP_TYPE),
                rs.getString("error_code"),
                rs.getString("error_message")
        );
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return limit;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    "ORCHESTRATION_SERIALIZATION_FAILED",
                    "failed to serialize orchestration execution payload",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex
            );
        }
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    "ORCHESTRATION_DESERIALIZATION_FAILED",
                    "failed to deserialize orchestration execution payload",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex
            );
        } catch (DataAccessException ex) {
            throw ex;
        }
    }
}
