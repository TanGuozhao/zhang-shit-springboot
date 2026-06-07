package com.example.platform.message.repository;

import com.example.platform.message.domain.ReceiverGroup;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReceiverGroupRepository {

    private final Map<String, ReceiverGroup> groups = new LinkedHashMap<>();

    public ReceiverGroupRepository() {
        save(new ReceiverGroup("OPS_TEAM", "Operations Team", List.of("ops@example.com", "ops.lead@example.com"), true));
        save(new ReceiverGroup("FINANCE_TEAM", "Finance Team", List.of("finance@example.com"), true));
        save(new ReceiverGroup("SMS_TESTERS", "SMS Testers", List.of("13800000000", "13900000000"), true));
    }

    public void save(ReceiverGroup group) {
        groups.put(group.groupCode(), group);
    }

    public Optional<ReceiverGroup> findByCode(String groupCode) {
        return Optional.ofNullable(groups.get(groupCode));
    }

    public List<ReceiverGroup> findAll() {
        return new ArrayList<>(groups.values());
    }
}
