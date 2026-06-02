package com.example.platform.log.repository;

import com.example.platform.log.domain.AlertEvent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class AlertRepository {

    private final List<AlertEvent> alerts = new CopyOnWriteArrayList<>(List.of(
            new AlertEvent("ALT-1001", "HIGH_ERROR_RATE", "HIGH", "OPEN", "topbiz error rate exceeded threshold"),
            new AlertEvent("ALT-1002", "MESSAGE_DELAY", "MEDIUM", "ACKED", "message queue delay exceeded threshold")
    ));

    public List<AlertEvent> findAll() {
        return List.copyOf(alerts);
    }

    public Optional<AlertEvent> findById(String alertId) {
        return alerts.stream().filter(alert -> alert.alertId().equals(alertId)).findFirst();
    }

    public void save(AlertEvent alertEvent) {
        alerts.removeIf(existing -> existing.alertId().equals(alertEvent.alertId()));
        alerts.add(alertEvent);
    }
}
