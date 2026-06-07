package com.example.platform.message.repository;

import com.example.platform.message.domain.MessageRecordQuery;
import com.example.platform.message.domain.MessageTask;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MessageTaskRepository {

    private final Map<String, MessageTask> tasks = new ConcurrentHashMap<>();

    public MessageTaskRepository() {
        MessageTask sentSeed = new MessageTask(
                "MSG-SEED-1001",
                "WELCOME",
                "User Welcome Notice",
                "EMAIL",
                "email-main",
                "SENT",
                "Welcome aboard",
                "Hello Platform Admin, welcome to the platform.",
                List.of("admin@example.com"),
                Map.of("userName", "Platform Admin"),
                "IMMEDIATE",
                null,
                null,
                null,
                Instant.parse("2026-06-07T06:00:00Z"),
                Instant.parse("2026-06-07T06:00:10Z"),
                Instant.parse("2026-06-07T06:00:10Z"),
                null,
                null,
                0,
                List.of()
        );
        MessageTask failedSeed = new MessageTask(
                "MSG-SEED-2001",
                "ALERT_NOTICE",
                "Alert Notice SMS",
                "SMS",
                "sms-primary",
                "FAILED",
                "Alert notice",
                "Alert ERR-409 triggered at 2026-06-07T05:58:00Z.",
                List.of("13800000000"),
                Map.of("alertCode", "ERR-409", "currentTime", "2026-06-07T05:58:00Z"),
                "IMMEDIATE",
                null,
                null,
                null,
                Instant.parse("2026-06-07T05:58:00Z"),
                Instant.parse("2026-06-07T05:58:30Z"),
                Instant.parse("2026-06-07T05:58:20Z"),
                "CHANNEL_DELIVERY_FAILED",
                "Carrier throttled the SMS request.",
                2,
                List.of()
        );
        MessageTask draftSeed = new MessageTask(
                "DRF-SEED-3001",
                "BILLING_REMINDER",
                "Billing Reminder Email",
                "EMAIL",
                "email-main",
                "DRAFT",
                "Invoice reminder",
                "Hi {{userName}}, your invoice INV-10086 is due on 2026-06-08.",
                List.of("finance@example.com"),
                Map.of("userName", "Finance User", "invoiceNo", "INV-10086", "dueDate", "2026-06-08"),
                "DRAFT",
                Instant.parse("2026-06-08T02:00:00Z"),
                null,
                null,
                Instant.parse("2026-06-07T05:00:00Z"),
                Instant.parse("2026-06-07T05:00:00Z"),
                null,
                null,
                null,
                0,
                List.of()
        );
        tasks.put(sentSeed.messageId(), sentSeed);
        tasks.put(failedSeed.messageId(), failedSeed);
        tasks.put(draftSeed.messageId(), draftSeed);
    }

    public void save(MessageTask task) {
        tasks.put(task.messageId(), task);
    }

    public Optional<MessageTask> findById(String messageId) {
        return Optional.ofNullable(tasks.get(messageId));
    }

    public List<MessageTask> findAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(MessageTask::createdAt).reversed())
                .toList();
    }

    public List<MessageTask> findByStatus(String status) {
        return findAll().stream()
                .filter(task -> task.status().equalsIgnoreCase(status))
                .toList();
    }

    public List<MessageTask> search(MessageRecordQuery query) {
        return findAll().stream()
                .filter(task -> !StringUtils.hasText(query.status()) || task.status().equalsIgnoreCase(query.status()))
                .filter(task -> !StringUtils.hasText(query.channel()) || task.channel().equalsIgnoreCase(query.channel()))
                .filter(task -> !StringUtils.hasText(query.receiver()) || task.receivers().stream()
                        .anyMatch(receiver -> receiver.equalsIgnoreCase(query.receiver())))
                .filter(task -> query.startTime() == null || !task.createdAt().isBefore(query.startTime()))
                .filter(task -> query.endTime() == null || !task.createdAt().isAfter(query.endTime()))
                .filter(task -> {
                    if (!StringUtils.hasText(query.keyword())) {
                        return true;
                    }
                    String keyword = query.keyword().toLowerCase();
                    return task.messageId().toLowerCase().contains(keyword)
                            || task.templateCode().toLowerCase().contains(keyword)
                            || task.subject().toLowerCase().contains(keyword)
                            || task.content().toLowerCase().contains(keyword)
                            || task.receivers().stream().anyMatch(receiver -> receiver.toLowerCase().contains(keyword))
                            || task.variables().values().stream().anyMatch(value -> value != null && value.toLowerCase().contains(keyword));
                })
                .toList();
    }
}
