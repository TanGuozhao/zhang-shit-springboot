package com.example.platform.topbiz.service;

import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.AlertRuleResponse;
import com.example.platform.log.dto.AlertRuleStatusUpdateRequest;
import com.example.platform.log.dto.AlertRuleUpsertRequest;
import com.example.platform.log.dto.AlertStatusUpdateRequest;
import com.example.platform.log.dto.ExportRequest;
import com.example.platform.log.dto.ExportResponse;
import com.example.platform.log.dto.ExportTaskResponse;
import com.example.platform.log.dto.InternalLogSearchRequest;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.LogIngestRequest;
import com.example.platform.log.dto.LogSearchResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.log.dto.RuntimeOverviewResponse;
import com.example.platform.topbiz.remote.LogServiceClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopbizLogGatewayService {

    private final LogServiceClient logServiceClient;
    private final RemoteCallSupport remoteCallSupport;

    public TopbizLogGatewayService(LogServiceClient logServiceClient,
                                   RemoteCallSupport remoteCallSupport) {
        this.logServiceClient = logServiceClient;
        this.remoteCallSupport = remoteCallSupport;
    }

    public LogEntryResponse ingest(LogIngestRequest request) {
        return remoteCallSupport.unwrap(logServiceClient.ingest(request));
    }

    public LogSearchResponse search(String keyword) {
        return remoteCallSupport.unwrap(logServiceClient.search(keyword));
    }

    public LogSearchResponse trace(String traceId) {
        return remoteCallSupport.unwrap(logServiceClient.trace(traceId));
    }

    public MetricsResponse metrics(String serviceName) {
        return remoteCallSupport.unwrap(logServiceClient.metrics(serviceName));
    }

    public List<AlertResponse> alerts() {
        return remoteCallSupport.unwrap(logServiceClient.alerts());
    }

    public void updateAlertStatus(String alertId, AlertStatusUpdateRequest request) {
        remoteCallSupport.ensureOk(logServiceClient.updateAlertStatus(alertId, request));
    }

    public ExportResponse createExport(ExportRequest request) {
        return remoteCallSupport.unwrap(logServiceClient.createExport(request));
    }

    public LogSearchResponse internalSearch(InternalLogSearchRequest request) {
        return remoteCallSupport.unwrap(logServiceClient.internalSearch(request));
    }

    public RuntimeOverviewResponse runtime() {
        return remoteCallSupport.unwrap(logServiceClient.runtime());
    }

    public List<AlertRuleResponse> alertRules() {
        return remoteCallSupport.unwrap(logServiceClient.alertRules());
    }

    public List<ExportTaskResponse> exports() {
        return remoteCallSupport.unwrap(logServiceClient.exports());
    }

    public AlertRuleResponse upsertAlertRule(AlertRuleUpsertRequest request) {
        return remoteCallSupport.unwrap(logServiceClient.upsertAlertRule(request));
    }

    public void updateAlertRuleEnabled(String ruleId, AlertRuleStatusUpdateRequest request) {
        remoteCallSupport.ensureOk(logServiceClient.updateAlertRuleEnabled(ruleId, request));
    }

    public void deleteRule(String ruleId) {
        remoteCallSupport.ensureOk(logServiceClient.deleteRule(ruleId));
    }

    public void flush() {
        remoteCallSupport.ensureOk(logServiceClient.flush());
    }

    public void evaluateAlerts() {
        remoteCallSupport.ensureOk(logServiceClient.evaluateAlerts());
    }

    public void runExports() {
        remoteCallSupport.ensureOk(logServiceClient.runExports());
    }

    public void cleanupExports() {
        remoteCallSupport.ensureOk(logServiceClient.cleanupExports());
    }
}
