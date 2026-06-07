package com.example.platform.log.repository;

import com.example.platform.log.domain.AlertEvent;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class AlertRepository {

    private final CopyOnWriteArrayList<AlertEvent> alerts = new CopyOnWriteArrayList<>();

    public List<AlertEvent> findAll() {
        return alerts.stream()
                .sorted(Comparator.comparing(AlertEvent::createdAt).reversed())
                .toList();
    }

    public Optional<AlertEvent> findById(String alertId) {
        return alerts.stream().filter(alert -> alert.alertId().equals(alertId)).findFirst();
    }

    public Optional<AlertEvent> findOpenByRuleIdAndSummary(String ruleId, String summary) {
        return alerts.stream()
                .filter(alert -> ruleId.equals(alert.ruleId()))
                .filter(alert -> "OPEN".equals(alert.status()))
                .filter(alert -> summary.equals(alert.summary()))
                .findFirst();
    }

    public void save(AlertEvent alertEvent) {
        alerts.removeIf(existing -> existing.alertId().equals(alertEvent.alertId()));
        alerts.add(alertEvent);
    }
}
