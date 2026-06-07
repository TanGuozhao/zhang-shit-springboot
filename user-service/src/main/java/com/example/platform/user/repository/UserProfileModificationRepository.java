package com.example.platform.user.repository;

import com.example.platform.user.domain.UserProfileModificationRecord;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class UserProfileModificationRepository {

    private final ConcurrentMap<Long, CopyOnWriteArrayList<UserProfileModificationRecord>> records = new ConcurrentHashMap<>();
    private final AtomicLong recordIdSequence = new AtomicLong(0L);

    public void append(Long userId, String modifyField, String oldValue, String newValue) {
        UserProfileModificationRecord record = new UserProfileModificationRecord(
                recordIdSequence.incrementAndGet(),
                userId,
                Instant.now(),
                modifyField,
                oldValue,
                newValue
        );
        records.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(record);
    }

    public List<UserProfileModificationRecord> findByUserId(Long userId) {
        return records.getOrDefault(userId, new CopyOnWriteArrayList<>())
                .stream()
                .sorted(Comparator.comparing(UserProfileModificationRecord::modifyTime).reversed())
                .toList();
    }
}
