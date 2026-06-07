package com.example.platform.topbiz.repository;

import com.example.platform.topbiz.domain.OrchestrationExecutionRecord;

import java.util.List;
import java.util.Optional;

public interface TopbizOrchestrationRepository {

    OrchestrationExecutionRecord save(OrchestrationExecutionRecord record);

    Optional<OrchestrationExecutionRecord> findById(String orchestrationId);

    List<OrchestrationExecutionRecord> findAll(String orchestrationType, String status, Integer limit);
}
