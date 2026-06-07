package com.example.platform.message.repository;

import com.example.platform.message.domain.InboxMessage;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InboxRepository {

    private final Map<String, InboxMessage> inboxMessages = new ConcurrentHashMap<>();

    public void save(InboxMessage inboxMessage) {
        inboxMessages.put(inboxMessage.inboxId(), inboxMessage);
    }

    public Optional<InboxMessage> findById(String inboxId) {
        return Optional.ofNullable(inboxMessages.get(inboxId));
    }

    public List<InboxMessage> findAll() {
        return inboxMessages.values().stream()
                .sorted(Comparator.comparing(InboxMessage::deliveredAt).reversed())
                .toList();
    }

    public List<InboxMessage> findByReceiver(String receiver) {
        return findAll().stream()
                .filter(message -> receiver == null || receiver.isBlank() || message.receiver().equalsIgnoreCase(receiver))
                .toList();
    }

    public long countAll() {
        return inboxMessages.size();
    }
}
