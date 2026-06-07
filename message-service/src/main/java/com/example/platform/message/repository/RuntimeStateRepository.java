package com.example.platform.message.repository;

import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class RuntimeStateRepository {

    private volatile Instant lastDispatchRunAt;
    private volatile Instant lastRetryRunAt;

    public Instant getLastDispatchRunAt() {
        return lastDispatchRunAt;
    }

    public void setLastDispatchRunAt(Instant lastDispatchRunAt) {
        this.lastDispatchRunAt = lastDispatchRunAt;
    }

    public Instant getLastRetryRunAt() {
        return lastRetryRunAt;
    }

    public void setLastRetryRunAt(Instant lastRetryRunAt) {
        this.lastRetryRunAt = lastRetryRunAt;
    }
}
