package com.example.platform.message.repository;

import com.example.platform.message.domain.SchedulePolicy;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SchedulePolicyRepository {

    private final Map<String, SchedulePolicy> policies = new LinkedHashMap<>();

    public SchedulePolicyRepository() {
        Instant now = Instant.parse("2026-06-07T00:00:00Z");
        save(new SchedulePolicy("HOURLY_ALERTS", "0 0 * * * *", "CRON", true, "Run on the hour.", now, now));
        save(new SchedulePolicy("ONCE_LATER", null, "ONCE", true, "One-time scheduled execution.", now, now));
    }

    public void save(SchedulePolicy policy) {
        policies.put(policy.policyCode(), policy);
    }

    public Optional<SchedulePolicy> findByCode(String policyCode) {
        return Optional.ofNullable(policies.get(policyCode));
    }

    public List<SchedulePolicy> findAll() {
        return new ArrayList<>(policies.values());
    }
}
