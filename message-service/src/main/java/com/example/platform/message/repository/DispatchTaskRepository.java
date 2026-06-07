package com.example.platform.message.repository;

import com.example.platform.message.domain.DispatchTask;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DispatchTaskRepository {

    private final Map<String, DispatchTask> tasks = new ConcurrentHashMap<>();

    public void save(DispatchTask task) {
        tasks.put(task.taskId(), task);
    }

    public Optional<DispatchTask> findById(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<DispatchTask> findAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(DispatchTask::createdAt).reversed())
                .toList();
    }

    public List<DispatchTask> findByMessageId(String messageId) {
        return findAll().stream()
                .filter(task -> task.messageId().equals(messageId))
                .toList();
    }

    public List<DispatchTask> findByBatchCode(String batchCode) {
        return findAll().stream()
                .filter(task -> batchCode != null && batchCode.equals(task.batchCode()))
                .toList();
    }

    public List<DispatchTask> findRunnable(Instant now) {
        return findAll().stream()
                .filter(task -> task.status().equals("PENDING"))
                .filter(task -> task.plannedAt() == null || !task.plannedAt().isAfter(now))
                .toList();
    }

    public long countByStatus(String status) {
        return tasks.values().stream().filter(task -> task.status().equals(status)).count();
    }

    public List<DispatchTask> search(String status, Instant startTime, Instant endTime) {
        return findAll().stream()
                .filter(task -> status == null || status.isBlank() || task.status().equalsIgnoreCase(status))
                .filter(task -> startTime == null || !task.createdAt().isBefore(startTime))
                .filter(task -> endTime == null || !task.createdAt().isAfter(endTime))
                .toList();
    }
}
