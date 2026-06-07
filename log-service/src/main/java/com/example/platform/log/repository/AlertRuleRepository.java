package com.example.platform.log.repository;

import com.example.platform.log.domain.AlertRule;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class AlertRuleRepository {

    private final CopyOnWriteArrayList<AlertRule> rules = new CopyOnWriteArrayList<>();

    public List<AlertRule> findAll() {
        return List.copyOf(rules);
    }

    public List<AlertRule> findEnabled() {
        return rules.stream().filter(AlertRule::enabled).toList();
    }

    public Optional<AlertRule> findById(String ruleId) {
        return rules.stream().filter(rule -> rule.ruleId().equals(ruleId)).findFirst();
    }

    public void save(AlertRule rule) {
        rules.removeIf(existing -> existing.ruleId().equals(rule.ruleId()));
        rules.add(rule);
    }

    public void delete(String ruleId) {
        rules.removeIf(existing -> existing.ruleId().equals(ruleId));
    }
}
