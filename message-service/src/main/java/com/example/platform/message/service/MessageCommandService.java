package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.domain.DispatchTask;
import com.example.platform.message.domain.MessageChannel;
import com.example.platform.message.domain.MessageTask;
import com.example.platform.message.domain.MessageTemplate;
import com.example.platform.message.domain.TaskBatch;
import com.example.platform.message.domain.VariableResolution;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.dto.ReceiverResolutionRequest;
import com.example.platform.message.dto.ReceiverResolutionResponse;
import com.example.platform.message.dto.ScheduleValidationRequest;
import com.example.platform.message.dto.ScheduleValidationResponse;
import com.example.platform.message.dto.TemplatePreviewRequest;
import com.example.platform.message.dto.TemplatePreviewResponse;
import com.example.platform.message.dto.VariableFillRequest;
import com.example.platform.message.dto.VariableFillResponse;
import com.example.platform.message.dto.VariableValidationRequest;
import com.example.platform.message.dto.VariableValidationResponse;
import com.example.platform.message.repository.DispatchTaskRepository;
import com.example.platform.message.repository.MessageTaskRepository;
import com.example.platform.message.repository.TaskBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MessageCommandService {

    private final MessageTaskRepository messageTaskRepository;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final TaskBatchRepository taskBatchRepository;
    private final MessageFlowSupportService messageFlowSupportService;
    private final MessageOperationsService messageOperationsService;

    public MessageCommandService(MessageTaskRepository messageTaskRepository,
                                 DispatchTaskRepository dispatchTaskRepository,
                                 TaskBatchRepository taskBatchRepository,
                                 MessageFlowSupportService messageFlowSupportService,
                                 MessageOperationsService messageOperationsService) {
        this.messageTaskRepository = messageTaskRepository;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.taskBatchRepository = taskBatchRepository;
        this.messageFlowSupportService = messageFlowSupportService;
        this.messageOperationsService = messageOperationsService;
    }

    public MessageResponse send(MessageSendRequest request) {
        MessageTemplate template = messageFlowSupportService.getEnabledTemplate(request.templateCode());
        validateTemplateChannel(template, request.channel());
        MessageChannel channel = messageFlowSupportService.getAvailableChannel(request.channel());
        ReceiverResolutionResponse receiverResolution = resolveReceivers(new ReceiverResolutionRequest(
                request.receivers(),
                request.receiverGroups()
        ), channel.channelType());
        VariableResolution variableResolution = messageFlowSupportService.resolveVariables(template, request.variables(), true);
        if (!variableResolution.errors().isEmpty()) {
            throw new BusinessException("VARIABLE_VALIDATION_FAILED", String.join("; ", variableResolution.errors()));
        }
        var schedulePlan = messageFlowSupportService.validateSchedule(
                request.dispatchType(),
                request.scheduledAt(),
                request.cronExpression(),
                request.schedulePolicyCode()
        );
        Instant now = Instant.now();
        String messageId = "MSG-" + UUID.randomUUID();
        String batchCode = receiverResolution.receivers().size() > 1 ? "BATCH-" + UUID.randomUUID() : null;
        MessageTask task = new MessageTask(
                messageId,
                template.templateCode(),
                template.templateName(),
                channel.channelCode(),
                StringUtils.hasText(request.channelAccountCode()) ? request.channelAccountCode() : channel.accountCode(),
                schedulePlan.dispatchType().equals("SCHEDULED") ? "SCHEDULED" : "PENDING",
                variableResolution.renderedSubject(),
                variableResolution.renderedContent(),
                receiverResolution.receivers(),
                variableResolution.resolvedVariables(),
                schedulePlan.dispatchType(),
                schedulePlan.scheduledAt(),
                schedulePlan.cronExpression(),
                batchCode,
                now,
                now,
                null,
                null,
                null,
                0,
                request.attachmentIds() == null ? List.of() : List.copyOf(request.attachmentIds())
        );
        messageTaskRepository.save(task);
        createDispatchArtifacts(task, channel, request.schedulePolicyCode(), schedulePlan.scheduledAt(), batchCode, now);
        if ("IMMEDIATE".equals(schedulePlan.dispatchType())) {
            messageOperationsService.runDispatchTasksForMessage(messageId);
        }
        return messageOperationsService.toMessageResponse(messageTaskRepository.findById(messageId).orElse(task));
    }

    public MessageResponse saveDraft(MessageDraftRequest request) {
        MessageTemplate template = messageFlowSupportService.getEnabledTemplate(request.templateCode());
        VariableResolution variableResolution = messageFlowSupportService.resolveVariables(template, request.variables(), false);
        String channelCode = StringUtils.hasText(request.channel()) ? request.channel() : template.channel();
        validateTemplateChannel(template, channelCode);
        MessageChannel channel = messageFlowSupportService.getAvailableChannel(channelCode);
        ReceiverResolutionResponse receiverResolution =
                (request.receivers() == null || request.receivers().isEmpty())
                        && (request.receiverGroups() == null || request.receiverGroups().isEmpty())
                        ? new ReceiverResolutionResponse(List.of(), List.of())
                        : messageFlowSupportService.resolveReceivers(
                                new ReceiverResolutionRequest(request.receivers(), request.receiverGroups()),
                                channel.channelType()
                        );
        Instant now = Instant.now();
        MessageTask task = new MessageTask(
                "DRF-" + UUID.randomUUID(),
                template.templateCode(),
                template.templateName(),
                channel.channelCode(),
                channel.accountCode(),
                "DRAFT",
                StringUtils.hasText(request.title()) ? request.title() : variableResolution.renderedSubject(),
                StringUtils.hasText(request.content()) ? request.content() : variableResolution.renderedContent(),
                receiverResolution.receivers(),
                variableResolution.resolvedVariables(),
                "DRAFT",
                request.scheduledAt(),
                request.cronExpression(),
                null,
                now,
                now,
                null,
                null,
                null,
                0,
                request.attachmentIds() == null ? List.of() : List.copyOf(request.attachmentIds())
        );
        messageTaskRepository.save(task);
        return messageOperationsService.toMessageResponse(task);
    }

    public TemplatePreviewResponse preview(String templateCode, TemplatePreviewRequest request) {
        VariableResolution resolution = messageFlowSupportService.resolveVariables(templateCode, request.variables(), false);
        return new TemplatePreviewResponse(
                templateCode,
                resolution.renderedSubject(),
                resolution.renderedContent(),
                resolution.resolvedVariables()
        );
    }

    public VariableFillResponse fillVariables(VariableFillRequest request) {
        VariableResolution resolution = messageFlowSupportService.resolveVariables(request.templateCode(), request.variables(), false);
        return new VariableFillResponse(
                request.templateCode(),
                resolution.renderedSubject(),
                resolution.renderedContent(),
                resolution.resolvedVariables()
        );
    }

    public VariableValidationResponse validateVariables(VariableValidationRequest request) {
        VariableResolution resolution = messageFlowSupportService.resolveVariables(request.templateCode(), request.variables(), true);
        return new VariableValidationResponse(
                request.templateCode(),
                resolution.errors().isEmpty(),
                resolution.resolvedVariables(),
                resolution.errors()
        );
    }

    public ScheduleValidationResponse validateSchedule(ScheduleValidationRequest request) {
        var schedulePlan = messageFlowSupportService.validateSchedule(
                request.dispatchType(),
                request.scheduledAt(),
                request.cronExpression(),
                null
        );
        return new ScheduleValidationResponse(
                true,
                schedulePlan.dispatchType(),
                schedulePlan.scheduledAt(),
                schedulePlan.cronExpression(),
                schedulePlan.description()
        );
    }

    public ReceiverResolutionResponse resolveReceivers(ReceiverResolutionRequest request, String channelType) {
        return messageFlowSupportService.resolveReceivers(request, channelType);
    }

    public InboxMessageResponse markInboxMessageRead(String inboxId) {
        return messageOperationsService.markInboxMessageRead(inboxId);
    }

    private void createDispatchArtifacts(MessageTask message,
                                         MessageChannel channel,
                                         String schedulePolicyCode,
                                         Instant plannedAt,
                                         String batchCode,
                                         Instant now) {
        List<DispatchTask> dispatchTasks = new ArrayList<>();
        int index = 0;
        for (String receiver : message.receivers()) {
            index++;
            dispatchTasks.add(new DispatchTask(
                    "DST-" + UUID.randomUUID(),
                    "TASK-" + message.messageId() + "-" + index,
                    message.messageId(),
                    receiver,
                    channel.channelCode(),
                    message.channelAccountCode(),
                    schedulePolicyCode,
                    batchCode,
                    plannedAt,
                    null,
                    "PENDING",
                    index,
                    null,
                    now,
                    now
            ));
        }
        dispatchTasks.forEach(dispatchTaskRepository::save);
        if (StringUtils.hasText(batchCode)) {
            taskBatchRepository.save(new TaskBatch(
                    batchCode,
                    dispatchTasks.size(),
                    0,
                    "PENDING",
                    now,
                    now
            ));
        }
    }

    private void validateTemplateChannel(MessageTemplate template, String channelCode) {
        if (!template.channel().equalsIgnoreCase(channelCode)) {
            throw new BusinessException("CHANNEL_TEMPLATE_MISMATCH", "template channel does not match requested channel");
        }
    }
}
