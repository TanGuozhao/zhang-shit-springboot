package com.example.platform.message.repository;

import com.example.platform.message.domain.MessageTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TemplateRepository {

    private final List<MessageTemplate> templates = List.of(
            new MessageTemplate("WELCOME", "Welcome Template", "EMAIL", "Hello {{userName}}, welcome to the platform.", true, List.of("userName")),
            new MessageTemplate("ALERT_NOTICE", "Alert Notice", "SMS", "Alert {{alertCode}} triggered.", true, List.of("alertCode"))
    );

    public List<MessageTemplate> findAll() {
        return templates;
    }

    public Optional<MessageTemplate> findByCode(String templateCode) {
        return templates.stream().filter(template -> template.templateCode().equals(templateCode)).findFirst();
    }
}
