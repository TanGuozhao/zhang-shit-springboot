package com.example.platform.message.repository;

import com.example.platform.message.domain.MessageTask;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MessageTaskRepository {

    private final Map<String, MessageTask> tasks = new ConcurrentHashMap<>();

    public MessageTaskRepository() {
        MessageTask seed = new MessageTask(
                "MSG-SEED-1001",
                "WELCOME",
                "EMAIL",
                "SENT",
                java.util.List.of("admin@example.com"),
                java.util.Map.of("userName", "Platform Admin"),
                Instant.now()
        );
        tasks.put(seed.messageId(), seed);
    }

    public void save(MessageTask task) {
        tasks.put(task.messageId(), task);
    }

    public Optional<MessageTask> findById(String messageId) {
        return Optional.ofNullable(tasks.get(messageId));
    }
}
