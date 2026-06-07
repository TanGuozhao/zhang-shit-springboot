package com.example.platform.log.repository;

import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AccessLogRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Repository
public class BufferedLogQueueRepository {

    private final BlockingQueue<AccessLogRecord> queue;
    private final int capacity;

    public BufferedLogQueueRepository(LogServiceProperties properties) {
        this.capacity = properties.buffer().capacity();
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offer(AccessLogRecord record) {
        return queue.offer(record);
    }

    public List<AccessLogRecord> drain(int maxSize) {
        List<AccessLogRecord> drained = new ArrayList<>(maxSize);
        queue.drainTo(drained, maxSize);
        return drained;
    }

    public int depth() {
        return queue.size();
    }

    public int capacity() {
        return capacity;
    }
}
