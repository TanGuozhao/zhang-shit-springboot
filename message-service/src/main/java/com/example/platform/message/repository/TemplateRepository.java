package com.example.platform.message.repository;

import com.example.platform.message.domain.MessageTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TemplateRepository {

    private final Map<String, MessageTemplate> templates = new LinkedHashMap<>();

    public TemplateRepository() {
        save(new MessageTemplate(
                "WELCOME",
                "User Welcome Notice",
                "EMAIL",
                "Welcome aboard",
                "Hello {{userName}}, welcome to the platform.",
                "Welcome email template for new users.",
                true,
                List.of("userName")
        ));
        save(new MessageTemplate(
                "ALERT_NOTICE",
                "Alert Notice SMS",
                "SMS",
                "Alert notice",
                "Alert {{alertCode}} triggered at {{currentTime}}.",
                "SMS template used by alerting flows.",
                true,
                List.of("alertCode", "currentTime")
        ));
        save(new MessageTemplate(
                "BILLING_REMINDER",
                "Billing Reminder Email",
                "EMAIL",
                "Invoice reminder",
                "Hi {{userName}}, your invoice {{invoiceNo}} is due on {{dueDate}}.",
                "Billing reminder email template.",
                true,
                List.of("userName", "invoiceNo", "dueDate")
        ));
        save(new MessageTemplate(
                "OPS_DIGEST",
                "Ops Digest Feishu",
                "FEISHU",
                "Ops digest",
                "Service {{serviceName}} reported {{errorCount}} errors today.",
                "Daily operations digest template.",
                true,
                List.of("serviceName", "errorCount")
        ));
    }

    public List<MessageTemplate> findAll() {
        return new ArrayList<>(templates.values());
    }

    public Optional<MessageTemplate> findByCode(String templateCode) {
        return Optional.ofNullable(templates.get(templateCode));
    }

    public void save(MessageTemplate template) {
        templates.put(template.templateCode(), template);
    }
}
