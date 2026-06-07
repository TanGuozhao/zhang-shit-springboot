package com.example.platform.topbiz.service;

import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.topbiz.remote.LogServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TopbizAuditService {

    private static final Logger log = LoggerFactory.getLogger(TopbizAuditService.class);

    private final LogServiceClient logServiceClient;
    private final TopbizTraceSupport topbizTraceSupport;

    public TopbizAuditService(LogServiceClient logServiceClient,
                              TopbizTraceSupport topbizTraceSupport) {
        this.logServiceClient = logServiceClient;
        this.topbizTraceSupport = topbizTraceSupport;
    }

    public boolean audit(String level, String message, Map<String, String> tags) {
        try {
            Map<String, String> safeTags = tags == null ? Map.of() : new LinkedHashMap<>(tags);
            logServiceClient.ingest(new LogIngestRequest(
                    "topbiz",
                    topbizTraceSupport.currentTraceId(),
                    level,
                    message,
                    "/api/topbiz/orchestrations",
                    200,
                    0L,
                    null,
                    null,
                    Instant.now(),
                    safeTags
            ));
            return true;
        } catch (Exception ex) {
            log.warn("topbiz audit log ingestion failed: {}", ex.getMessage());
            return false;
        }
    }
}
