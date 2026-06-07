package com.example.platform.log.repository;

import com.example.platform.log.domain.ExportTask;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class ExportTaskRepository {

    private final CopyOnWriteArrayList<ExportTask> tasks = new CopyOnWriteArrayList<>();

    public void save(ExportTask task) {
        tasks.removeIf(existing -> existing.exportId().equals(task.exportId()));
        tasks.add(task);
    }

    public Optional<ExportTask> findById(String exportId) {
        return tasks.stream().filter(task -> task.exportId().equals(exportId)).findFirst();
    }

    public List<ExportTask> findAll() {
        return tasks.stream()
                .sorted(Comparator.comparing(ExportTask::createdAt).reversed())
                .toList();
    }

    public List<ExportTask> findByStatus(String status) {
        return tasks.stream().filter(task -> status.equals(task.status())).toList();
    }

    public List<ExportTask> findExpiredCompletedBefore(Instant threshold) {
        return tasks.stream()
                .filter(task -> "COMPLETED".equals(task.status()))
                .filter(task -> task.expiresAt() != null && !task.expiresAt().isAfter(threshold))
                .toList();
    }
}
