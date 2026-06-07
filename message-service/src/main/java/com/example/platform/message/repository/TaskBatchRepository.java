package com.example.platform.message.repository;

import com.example.platform.message.domain.TaskBatch;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TaskBatchRepository {

    private final Map<String, TaskBatch> batches = new ConcurrentHashMap<>();

    public void save(TaskBatch batch) {
        batches.put(batch.batchCode(), batch);
    }

    public Optional<TaskBatch> findByCode(String batchCode) {
        return Optional.ofNullable(batches.get(batchCode));
    }
}
