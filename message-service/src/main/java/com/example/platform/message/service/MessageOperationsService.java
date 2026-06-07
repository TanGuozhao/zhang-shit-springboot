package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.config.MessageServiceProperties;
import com.example.platform.message.domain.DispatchTask;
import com.example.platform.message.domain.InboxMessage;
import com.example.platform.message.domain.MessageTask;
import com.example.platform.message.domain.RetryRecord;
import com.example.platform.message.domain.TaskBatch;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.TaskExecutionResponse;
import com.example.platform.message.repository.DispatchTaskRepository;
import com.example.platform.message.repository.InboxRepository;
import com.example.platform.message.repository.MessageTaskRepository;
import com.example.platform.message.repository.RetryRecordRepository;
import com.example.platform.message.repository.RuntimeStateRepository;
import com.example.platform.message.repository.TaskBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MessageOperationsService {

    private final MessageTaskRepository messageTaskRepository;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final RetryRecordRepository retryRecordRepository;
    private final InboxRepository inboxRepository;
    private final RuntimeStateRepository runtimeStateRepository;
    private final TaskBatchRepository taskBatchRepository;
    private final MessageFlowSupportService messageFlowSupportService;
    private final MessageServiceProperties messageServiceProperties;

    public MessageOperationsService(MessageTaskRepository messageTaskRepository,
                                    DispatchTaskRepository dispatchTaskRepository,
                                    RetryRecordRepository retryRecordRepository,
                                    InboxRepository inboxRepository,
                                    RuntimeStateRepository runtimeStateRepository,
                                    TaskBatchRepository taskBatchRepository,
                                    MessageFlowSupportService messageFlowSupportService,
                                    MessageServiceProperties messageServiceProperties) {
        this.messageTaskRepository = messageTaskRepository;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.retryRecordRepository = retryRecordRepository;
        this.inboxRepository = inboxRepository;
        this.runtimeStateRepository = runtimeStateRepository;
        this.taskBatchRepository = taskBatchRepository;
        this.messageFlowSupportService = messageFlowSupportService;
        this.messageServiceProperties = messageServiceProperties;
    }

    public TaskExecutionResponse runPendingDispatchTasks() {
        return runDispatchTasks(dispatchTaskRepository.findRunnable(Instant.now()));
    }

    public TaskExecutionResponse runDispatchTask(String taskId) {
        DispatchTask task = dispatchTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("DISPATCH_TASK_NOT_FOUND", "dispatch task not found"));
        return runDispatchTasks(List.of(task));
    }

    public TaskExecutionResponse runDispatchTasksForMessage(String messageId) {
        return runDispatchTasks(dispatchTaskRepository.findByMessageId(messageId));
    }

    public TaskExecutionResponse runPendingRetries() {
        Instant now = Instant.now();
        int processed = 0;
        int success = 0;
        int failure = 0;
        int skipped = 0;
        for (RetryRecord retryRecord : retryRecordRepository.findPending()) {
            processed++;
            if (retryRecord.retryCount() >= messageServiceProperties.maxRetryAttempts()) {
                retryRecordRepository.save(new RetryRecord(
                        retryRecord.retryId(),
                        retryRecord.messageId(),
                        retryRecord.dispatchTaskId(),
                        retryRecord.retryCount(),
                        "FAILED",
                        now,
                        "retry attempts exhausted",
                        retryRecord.createdAt(),
                        now
                ));
                failure++;
                continue;
            }
            DispatchTask dispatchTask = dispatchTaskRepository.findById(retryRecord.dispatchTaskId())
                    .orElse(null);
            if (dispatchTask == null || dispatchTask.status().equals("CANCELLED")) {
                retryRecordRepository.save(new RetryRecord(
                        retryRecord.retryId(),
                        retryRecord.messageId(),
                        retryRecord.dispatchTaskId(),
                        retryRecord.retryCount(),
                        "CANCELLED",
                        now,
                        dispatchTask == null ? "dispatch task missing" : "dispatch task cancelled",
                        retryRecord.createdAt(),
                        now
                ));
                skipped++;
                continue;
            }
            dispatchTaskRepository.save(new DispatchTask(
                    dispatchTask.taskId(),
                    dispatchTask.taskCode(),
                    dispatchTask.messageId(),
                    dispatchTask.receiver(),
                    dispatchTask.channelCode(),
                    dispatchTask.channelAccountCode(),
                    dispatchTask.schedulePolicyCode(),
                    dispatchTask.batchCode(),
                    now,
                    null,
                    "PENDING",
                    dispatchTask.sortOrder(),
                    null,
                    dispatchTask.createdAt(),
                    now
            ));
            TaskExecutionResponse execution = runDispatchTask(dispatchTask.taskId());
            if (execution.successCount() > 0) {
                retryRecordRepository.save(new RetryRecord(
                        retryRecord.retryId(),
                        retryRecord.messageId(),
                        retryRecord.dispatchTaskId(),
                        retryRecord.retryCount() + 1,
                        "SUCCESS",
                        now,
                        null,
                        retryRecord.createdAt(),
                        now
                ));
                success++;
            } else {
                RetryRecord updatedRecord = new RetryRecord(
                        retryRecord.retryId(),
                        retryRecord.messageId(),
                        retryRecord.dispatchTaskId(),
                        retryRecord.retryCount() + 1,
                        retryRecord.retryCount() + 1 >= messageServiceProperties.maxRetryAttempts() ? "FAILED" : "PENDING",
                        now,
                        "dispatch retry still failed",
                        retryRecord.createdAt(),
                        now
                );
                retryRecordRepository.save(updatedRecord);
                failure++;
            }
        }
        runtimeStateRepository.setLastRetryRunAt(now);
        return new TaskExecutionResponse(processed, success, failure, skipped, now);
    }

    public InboxMessageResponse markInboxMessageRead(String inboxId) {
        InboxMessage inboxMessage = inboxRepository.findById(inboxId)
                .orElseThrow(() -> new BusinessException("INBOX_MESSAGE_NOT_FOUND", "inbox message not found"));
        InboxMessage updated = new InboxMessage(
                inboxMessage.inboxId(),
                inboxMessage.messageId(),
                inboxMessage.receiver(),
                inboxMessage.channel(),
                inboxMessage.subject(),
                inboxMessage.content(),
                "READ",
                inboxMessage.deliveredAt(),
                Instant.now()
        );
        inboxRepository.save(updated);
        return new InboxMessageResponse(
                updated.inboxId(),
                updated.messageId(),
                updated.receiver(),
                updated.channel(),
                updated.subject(),
                updated.content(),
                updated.readStatus(),
                updated.deliveredAt(),
                updated.readAt()
        );
    }

    public void synchronizeMessageState(String messageId, String batchCode) {
        Instant now = Instant.now();
        refreshBatchStatus(batchCode, now);
        refreshMessageStatus(messageId);
    }

    public MessageResponse toMessageResponse(MessageTask task) {
        return new MessageResponse(
                task.messageId(),
                task.templateCode(),
                task.templateName(),
                task.channel(),
                task.status(),
                task.subject(),
                task.content(),
                task.receivers(),
                task.variables(),
                task.dispatchType(),
                task.scheduledAt(),
                task.cronExpression(),
                task.batchCode(),
                task.createdAt(),
                task.updatedAt(),
                task.sentAt(),
                task.attachmentIds()
        );
    }

    private TaskExecutionResponse runDispatchTasks(List<DispatchTask> tasks) {
        Instant now = Instant.now();
        int processed = 0;
        int success = 0;
        int failure = 0;
        int skipped = 0;
        List<String> processedMessages = new ArrayList<>();
        for (DispatchTask task : tasks) {
            if (!(task.status().equals("PENDING") || task.status().equals("FAILED"))) {
                skipped++;
                continue;
            }
            processed++;
            MessageTask message = messageTaskRepository.findById(task.messageId())
                    .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found"));
            DispatchTask processing = new DispatchTask(
                    task.taskId(),
                    task.taskCode(),
                    task.messageId(),
                    task.receiver(),
                    task.channelCode(),
                    task.channelAccountCode(),
                    task.schedulePolicyCode(),
                    task.batchCode(),
                    task.plannedAt(),
                    task.actualAt(),
                    "PROCESSING",
                    task.sortOrder(),
                    null,
                    task.createdAt(),
                    now
            );
            dispatchTaskRepository.save(processing);
            refreshBatchStatus(processing.batchCode(), now);
            processedMessages.add(message.messageId());
            int attemptCount = message.retryCount();
            if (messageFlowSupportService.isPermanentFailure(message.variables(), task.receiver())) {
                failDispatch(processing, message, "CHANNEL_PERMANENT_FAILURE", "receiver is configured to fail permanently", now, true);
                failure++;
                continue;
            }
            if (messageFlowSupportService.isTransientFailure(message.variables(), task.receiver(), attemptCount)) {
                failDispatch(processing, message, "CHANNEL_DELIVERY_FAILED", "transient carrier failure", now, false);
                failure++;
                continue;
            }
            completeDispatch(processing, message, now);
            success++;
        }
        for (String messageId : processedMessages.stream().distinct().toList()) {
            refreshMessageStatus(messageId);
        }
        runtimeStateRepository.setLastDispatchRunAt(now);
        return new TaskExecutionResponse(processed, success, failure, skipped, now);
    }

    private void completeDispatch(DispatchTask task, MessageTask message, Instant now) {
        dispatchTaskRepository.save(new DispatchTask(
                task.taskId(),
                task.taskCode(),
                task.messageId(),
                task.receiver(),
                task.channelCode(),
                task.channelAccountCode(),
                task.schedulePolicyCode(),
                task.batchCode(),
                task.plannedAt(),
                now,
                "COMPLETED",
                task.sortOrder(),
                null,
                task.createdAt(),
                now
        ));
        refreshBatchStatus(task.batchCode(), now);
        inboxRepository.save(new InboxMessage(
                "INBOX-" + UUID.randomUUID(),
                message.messageId(),
                task.receiver(),
                message.channel(),
                message.subject(),
                message.content(),
                "UNREAD",
                now,
                null
        ));
    }

    private void failDispatch(DispatchTask task,
                              MessageTask message,
                              String errorCode,
                              String errorReason,
                              Instant now,
                              boolean permanent) {
        dispatchTaskRepository.save(new DispatchTask(
                task.taskId(),
                task.taskCode(),
                task.messageId(),
                task.receiver(),
                task.channelCode(),
                task.channelAccountCode(),
                task.schedulePolicyCode(),
                task.batchCode(),
                task.plannedAt(),
                now,
                permanent ? "FAILED" : "FAILED",
                task.sortOrder(),
                errorReason,
                task.createdAt(),
                now
        ));
        MessageTask failedMessage = new MessageTask(
                message.messageId(),
                message.templateCode(),
                message.templateName(),
                message.channel(),
                message.channelAccountCode(),
                "FAILED",
                message.subject(),
                message.content(),
                message.receivers(),
                message.variables(),
                message.dispatchType(),
                message.scheduledAt(),
                message.cronExpression(),
                message.batchCode(),
                message.createdAt(),
                now,
                message.sentAt(),
                errorCode,
                errorReason,
                message.retryCount() + 1,
                message.attachmentIds()
        );
        messageTaskRepository.save(failedMessage);
        if (!permanent && retryRecordRepository.findPendingByDispatchTaskId(task.taskId()).isEmpty()) {
            retryRecordRepository.save(new RetryRecord(
                    "RTR-" + UUID.randomUUID(),
                    message.messageId(),
                    task.taskId(),
                    message.retryCount(),
                    "PENDING",
                    null,
                    errorReason,
                    now,
                    now
            ));
        }
        refreshBatchStatus(task.batchCode(), now);
    }

    private void refreshMessageStatus(String messageId) {
        MessageTask message = messageTaskRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found"));
        List<DispatchTask> tasks = dispatchTaskRepository.findByMessageId(messageId);
        boolean anyFailed = tasks.stream().anyMatch(task -> task.status().equals("FAILED"));
        boolean allCompleted = !tasks.isEmpty() && tasks.stream().allMatch(task -> task.status().equals("COMPLETED"));
        boolean allCancelled = !tasks.isEmpty() && tasks.stream().allMatch(task -> task.status().equals("CANCELLED"));
        Instant now = Instant.now();
        if (allCompleted) {
            messageTaskRepository.save(new MessageTask(
                    message.messageId(),
                    message.templateCode(),
                    message.templateName(),
                    message.channel(),
                    message.channelAccountCode(),
                    "SENT",
                    message.subject(),
                    message.content(),
                    message.receivers(),
                    message.variables(),
                    message.dispatchType(),
                    message.scheduledAt(),
                    message.cronExpression(),
                    message.batchCode(),
                    message.createdAt(),
                    now,
                    now,
                    null,
                    null,
                    message.retryCount(),
                    message.attachmentIds()
            ));
        } else if (allCancelled) {
            messageTaskRepository.save(new MessageTask(
                    message.messageId(),
                    message.templateCode(),
                    message.templateName(),
                    message.channel(),
                    message.channelAccountCode(),
                    "CANCELLED",
                    message.subject(),
                    message.content(),
                    message.receivers(),
                    message.variables(),
                    message.dispatchType(),
                    message.scheduledAt(),
                    message.cronExpression(),
                    message.batchCode(),
                    message.createdAt(),
                    now,
                    message.sentAt(),
                    null,
                    "all dispatch tasks were cancelled",
                    message.retryCount(),
                    message.attachmentIds()
            ));
        } else if (anyFailed) {
            MessageTask failed = messageTaskRepository.findById(messageId).orElse(message);
            if (!"FAILED".equals(failed.status())) {
                messageTaskRepository.save(new MessageTask(
                        message.messageId(),
                        message.templateCode(),
                        message.templateName(),
                        message.channel(),
                        message.channelAccountCode(),
                        "FAILED",
                        message.subject(),
                        message.content(),
                        message.receivers(),
                        message.variables(),
                        message.dispatchType(),
                        message.scheduledAt(),
                        message.cronExpression(),
                        message.batchCode(),
                        message.createdAt(),
                        now,
                        message.sentAt(),
                        message.errorCode(),
                        message.errorReason(),
                        message.retryCount(),
                        message.attachmentIds()
                ));
            }
        }
    }

    private void refreshBatchStatus(String batchCode, Instant now) {
        if (!StringUtils.hasText(batchCode)) {
            return;
        }
        List<DispatchTask> batchTasks = dispatchTaskRepository.findByBatchCode(batchCode);
        if (batchTasks.isEmpty()) {
            return;
        }
        int processedCount = (int) batchTasks.stream()
                .filter(task -> !"PENDING".equals(task.status()))
                .count();
        String status;
        if (batchTasks.stream().allMatch(task -> "CANCELLED".equals(task.status()))) {
            status = "CANCELLED";
        } else if (batchTasks.stream().allMatch(task -> "COMPLETED".equals(task.status()))) {
            status = "COMPLETED";
        } else if (batchTasks.stream().anyMatch(task -> "PROCESSING".equals(task.status()))) {
            status = "PROCESSING";
        } else if (batchTasks.stream().anyMatch(task -> "FAILED".equals(task.status()))) {
            status = "PARTIAL_FAILED";
        } else if (processedCount > 0) {
            status = "PROCESSING";
        } else {
            status = "PENDING";
        }
        TaskBatch existing = taskBatchRepository.findByCode(batchCode)
                .orElse(new TaskBatch(batchCode, batchTasks.size(), 0, "PENDING", now, now));
        taskBatchRepository.save(new TaskBatch(
                existing.batchCode(),
                existing.totalTaskCount(),
                processedCount,
                status,
                existing.createdAt(),
                now
        ));
    }
}
