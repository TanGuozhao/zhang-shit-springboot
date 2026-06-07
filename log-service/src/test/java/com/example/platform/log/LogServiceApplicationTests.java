package com.example.platform.log;

import com.example.platform.log.dto.AlertRuleStatusUpdateRequest;
import com.example.platform.log.dto.AlertRuleUpsertRequest;
import com.example.platform.log.dto.ExportRequest;
import com.example.platform.log.dto.InternalLogSearchRequest;
import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.service.LogOperationsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "platform.log.scheduler.enabled=false",
        "platform.log.storage.clickhouse-enabled=false",
        "platform.log.export.directory=build/test-log-exports",
        "platform.log.export.file-ttl-hours=0"
})
class LogServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LogOperationsService logOperationsService;

    @Test
    void ingestFlushAndPublicSearchShouldWork() throws Exception {
        LogIngestRequest request = new LogIngestRequest(
                "topbiz",
                "TRACE-P2-1",
                "INFO",
                "order created successfully",
                "/orders",
                200,
                45L,
                "REQ-1",
                "127.0.0.1",
                null,
                Map.of("module", "order")
        );

        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logId").isNotEmpty())
                .andExpect(jsonPath("$.data.traceId").value("TRACE-P2-1"));

        logOperationsService.flushQueuedLogs();

        mockMvc.perform(get("/api/logs/search").param("keyword", "order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].serviceName").value("topbiz"));

        mockMvc.perform(get("/api/logs/trace/{traceId}", "TRACE-P2-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].traceId").value("TRACE-P2-1"));
    }

    @Test
    void internalSearchAndMetricsShouldUseFlushedLogs() throws Exception {
        List<LogIngestRequest> requests = List.of(
                new LogIngestRequest("topbiz", "TRACE-M-1", "INFO", "metrics info", "/metrics", 200, 30L, null, null, null, Map.of()),
                new LogIngestRequest("topbiz", "TRACE-M-2", "ERROR", "metrics error", "/metrics", 500, 130L, null, null, null, Map.of()),
                new LogIngestRequest("message-service", "TRACE-M-3", "INFO", "other service", "/messages", 200, 50L, null, null, null, Map.of())
        );
        for (LogIngestRequest request : requests) {
            mockMvc.perform(post("/api/logs/ingest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
        logOperationsService.flushQueuedLogs();

        InternalLogSearchRequest searchRequest = new InternalLogSearchRequest(
                "metrics",
                "topbiz",
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );

        mockMvc.perform(post("/api/logs/internal/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        String metricsResponse = mockMvc.perform(get("/api/logs/metrics").param("serviceName", "topbiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.totalRequests").value(2))
                .andExpect(jsonPath("$.data.metrics.errorRate").value(50.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> payload = objectMapper.readValue(metricsResponse, new TypeReference<>() {
        });
        assertThat(payload).isNotEmpty();
    }

    @Test
    void ingestAndInternalSearchShouldRejectInvalidGuardrails() throws Exception {
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogIngestRequest(
                                "topbiz",
                                "TRACE-INVALID-1",
                                "TRACE",
                                "invalid level",
                                "/orders",
                                200,
                                10L,
                                null,
                                null,
                                null,
                                Map.of()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LOG_LEVEL"));

        InternalLogSearchRequest request = new InternalLogSearchRequest(
                "metrics",
                "topbiz",
                null,
                null,
                null,
                Instant.now().minus(Duration.ofDays(8)),
                Instant.now(),
                0,
                20
        );

        mockMvc.perform(post("/api/logs/internal/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_WINDOW_EXCEEDED"));
    }

    @Test
    void alertRuleEvaluationAndStatusUpdateShouldWork() throws Exception {
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogIngestRequest(
                                "topbiz",
                                "TRACE-A-1",
                                "ERROR",
                                "db timeout",
                                "/orders",
                                500,
                                220L,
                                null,
                                null,
                                null,
                                Map.of()
                        ))))
                .andExpect(status().isOk());
        logOperationsService.flushQueuedLogs();

        AlertRuleUpsertRequest ruleRequest = new AlertRuleUpsertRequest(
                "topbiz error rule",
                "ERROR_COUNT",
                "topbiz",
                1,
                60,
                true,
                List.of("CONSOLE")
        );

        String createRuleResponse = mockMvc.perform(post("/api/logs/internal/alert-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> createRulePayload = objectMapper.readValue(createRuleResponse, new TypeReference<>() {
        });
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleData = (Map<String, Object>) createRulePayload.get("data");
        String ruleId = (String) ruleData.get("ruleId");

        mockMvc.perform(post("/api/logs/internal/tasks/alerts/evaluate"))
                .andExpect(status().isOk());

        String alertsResponse = mockMvc.perform(get("/api/logs/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> alertsPayload = objectMapper.readValue(alertsResponse, new TypeReference<>() {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) alertsPayload.get("data");
        String alertId = (String) alerts.getFirst().get("alertId");

        mockMvc.perform(post("/api/logs/alerts/{alertId}/status", alertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACKED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/logs/internal/alert-rules/{ruleId}/enabled", ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AlertRuleStatusUpdateRequest(false))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/logs/internal/alert-rules/{ruleId}", ruleId))
                .andExpect(status().isOk());
    }

    @Test
    void exportTaskShouldBeGeneratedAsynchronously() throws Exception {
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogIngestRequest(
                                "topbiz",
                                "TRACE-E-1",
                                "INFO",
                                "export me 13800138000 token=secret-value",
                                "/export",
                                200,
                                10L,
                                null,
                                null,
                                null,
                                Map.of()
                        ))))
                .andExpect(status().isOk());
        logOperationsService.flushQueuedLogs();

        String createExportResponse = mockMvc.perform(post("/api/logs/exports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExportRequest("CSV", "export"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        logOperationsService.runPendingExports();

        Map<String, Object> exportPayload = objectMapper.readValue(createExportResponse, new TypeReference<>() {
        });
        @SuppressWarnings("unchecked")
        Map<String, Object> exportData = (Map<String, Object>) exportPayload.get("data");
        String path = (String) exportData.get("downloadPath");
        assertThat(Files.exists(Path.of(path.replace("/downloads/", "build/test-log-exports/")))).isTrue();
        assertThat(Files.readString(Path.of(path.replace("/downloads/", "build/test-log-exports/"))))
                .contains("138****8000")
                .contains("token=***");

        mockMvc.perform(get(path))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/logs/internal/exports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].recordCount").value(1));

        logOperationsService.cleanupExpiredExports();
        assertThat(Files.exists(Path.of(path.replace("/downloads/", "build/test-log-exports/")))).isFalse();

        mockMvc.perform(get("/api/logs/internal/exports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("EXPIRED"));

        mockMvc.perform(get("/api/logs/internal/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedExports").value(0))
                .andExpect(jsonPath("$.data.expiredExports").value(1));
    }

    @Test
    void latencyAlertShouldRespectRuleWindow() throws Exception {
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogIngestRequest(
                                "topbiz",
                                "TRACE-L-OLD",
                                "INFO",
                                "older slow request",
                                "/orders",
                                200,
                                600L,
                                null,
                                null,
                                Instant.now().minus(Duration.ofMinutes(30)),
                                Map.of()
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogIngestRequest(
                                "topbiz",
                                "TRACE-L-NEW",
                                "INFO",
                                "recent fast request",
                                "/orders",
                                200,
                                20L,
                                null,
                                null,
                                Instant.now(),
                                Map.of()
                        ))))
                .andExpect(status().isOk());
        logOperationsService.flushQueuedLogs();

        mockMvc.perform(post("/api/logs/internal/alert-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AlertRuleUpsertRequest(
                                "recent p95 rule",
                                "LATENCY_P95",
                                "topbiz",
                                100,
                                1,
                                true,
                                List.of("CONSOLE")
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/logs/internal/tasks/alerts/evaluate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/logs/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
