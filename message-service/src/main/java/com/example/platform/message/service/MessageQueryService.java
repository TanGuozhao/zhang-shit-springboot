package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.config.MessageServiceProperties;
import com.example.platform.message.domain.InboxMessage;
import com.example.platform.message.domain.MessageRecordQuery;
import com.example.platform.message.domain.MessageTask;
import com.example.platform.message.domain.MessageTemplate;
import com.example.platform.message.domain.MessageVariable;
import com.example.platform.message.dto.ChannelResponse;
import com.example.platform.message.dto.InboxListResponse;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageErrorResponse;
import com.example.platform.message.dto.MessageRecordResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageStatisticsResponse;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.RuntimeOverviewResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.repository.ChannelRepository;
import com.example.platform.message.repository.DispatchTaskRepository;
import com.example.platform.message.repository.InboxRepository;
import com.example.platform.message.repository.MessageTaskRepository;
import com.example.platform.message.repository.RetryRecordRepository;
import com.example.platform.message.repository.RuntimeStateRepository;
import com.example.platform.message.repository.TemplateRepository;
import com.example.platform.message.repository.VariableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageQueryService {

    private final MessageTaskRepository messageTaskRepository;
    private final TemplateRepository templateRepository;
    private final VariableRepository variableRepository;
    private final ChannelRepository channelRepository;
    private final InboxRepository inboxRepository;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final RetryRecordRepository retryRecordRepository;
    private final RuntimeStateRepository runtimeStateRepository;
    private final MessageServiceProperties messageServiceProperties;

    public MessageQueryService(MessageTaskRepository messageTaskRepository,
                               TemplateRepository templateRepository,
                               VariableRepository variableRepository,
                               ChannelRepository channelRepository,
                               InboxRepository inboxRepository,
                               DispatchTaskRepository dispatchTaskRepository,
                               RetryRecordRepository retryRecordRepository,
                               RuntimeStateRepository runtimeStateRepository,
                               MessageServiceProperties messageServiceProperties) {
        this.messageTaskRepository = messageTaskRepository;
        this.templateRepository = templateRepository;
        this.variableRepository = variableRepository;
        this.channelRepository = channelRepository;
        this.inboxRepository = inboxRepository;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.retryRecordRepository = retryRecordRepository;
        this.runtimeStateRepository = runtimeStateRepository;
        this.messageServiceProperties = messageServiceProperties;
    }

    public MessageResponse getMessage(String messageId) {
        MessageTask task = messageTaskRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found", HttpStatus.NOT_FOUND));
        return toResponse(task);
    }

    public MessageStatusResponse getStatus(String messageId) {
        MessageTask task = messageTaskRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found", HttpStatus.NOT_FOUND));
        return new MessageStatusResponse(
                task.messageId(),
                task.status(),
                task.dispatchType(),
                task.scheduledAt(),
                task.sentAt(),
                task.updatedAt()
        );
    }

    public MessageErrorResponse getError(String messageId) {
        MessageTask task = messageTaskRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found", HttpStatus.NOT_FOUND));
        return new MessageErrorResponse(
                task.messageId(),
                task.errorCode(),
                task.errorReason(),
                task.retryCount() > 0,
                task.retryCount(),
                task.updatedAt()
        );
    }

    public List<TemplateSummaryResponse> listTemplates(String channel) {
        return templateRepository.findAll().stream()
                .filter(template -> channel == null || channel.isBlank() || template.channel().equalsIgnoreCase(channel))
                .map(template -> new TemplateSummaryResponse(
                        template.templateCode(),
                        template.templateName(),
                        template.channel(),
                        template.description(),
                        template.enabled()
                ))
                .toList();
    }

    public TemplateDetailResponse getTemplate(String templateCode) {
        MessageTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "template not found", HttpStatus.NOT_FOUND));
        return new TemplateDetailResponse(
                template.templateCode(),
                template.templateName(),
                template.channel(),
                template.subjectTemplate(),
                template.contentTemplate(),
                template.description(),
                template.enabled(),
                getTemplateVariables(templateCode)
        );
    }

    public List<VariableDefinitionResponse> getTemplateVariables(String templateCode) {
        MessageTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "template not found", HttpStatus.NOT_FOUND));
        return variableRepository.findAllByCodes(template.variableCodes()).stream()
                .map(this::toVariableResponse)
                .toList();
    }

    public List<ChannelResponse> listChannels() {
        return channelRepository.findAll().stream()
                .map(channel -> new ChannelResponse(
                        channel.channelCode(),
                        channel.channelType(),
                        channel.carrierName(),
                        channel.accountCode(),
                        channel.sender(),
                        channel.enabled(),
                        channel.healthy(),
                        channel.description()
                ))
                .toList();
    }

    public MessageRecordResponse listRecords(String status,
                                             String channel,
                                             String keyword,
                                             String receiver,
                                             Instant startTime,
                                             Instant endTime) {
        List<MessageResponse> records = messageTaskRepository.search(new MessageRecordQuery(
                        status,
                        channel,
                        keyword,
                        receiver,
                        startTime,
                        endTime
                )).stream()
                .map(this::toResponse)
                .toList();
        return new MessageRecordResponse(records, records.size());
    }

    public InboxListResponse listInbox(String receiver) {
        List<InboxMessageResponse> records = inboxRepository.findByReceiver(receiver).stream()
                .map(this::toInboxResponse)
                .toList();
        return new InboxListResponse(records, records.size());
    }

    public InboxMessageResponse getInboxMessage(String inboxId) {
        InboxMessage inboxMessage = inboxRepository.findById(inboxId)
                .orElseThrow(() -> new BusinessException("INBOX_MESSAGE_NOT_FOUND", "inbox message not found", HttpStatus.NOT_FOUND));
        return toInboxResponse(inboxMessage);
    }

    public RuntimeOverviewResponse getRuntimeOverview() {
        return new RuntimeOverviewResponse(
                dispatchTaskRepository.countByStatus("PENDING"),
                dispatchTaskRepository.countByStatus("PROCESSING"),
                retryRecordRepository.countPending(),
                inboxRepository.countAll(),
                messageServiceProperties.schedulerEnabled(),
                messageServiceProperties.retryEnabled(),
                runtimeStateRepository.getLastDispatchRunAt(),
                runtimeStateRepository.getLastRetryRunAt()
        );
    }

    public MessageStatisticsResponse getStatistics(Instant startTime, Instant endTime) {
        List<MessageTask> records = messageTaskRepository.search(new MessageRecordQuery(
                null,
                null,
                null,
                null,
                startTime,
                endTime
        ));
        long total = records.size();
        long successful = records.stream().filter(record -> record.status().equals("SENT")).count();
        long failed = records.stream().filter(record -> record.status().equals("FAILED")).count();
        long scheduled = records.stream().filter(record -> record.dispatchType().equals("SCHEDULED")).count();
        double successRate = total == 0 ? 0 : ((double) successful / (double) total);
        Map<String, Long> channelBreakdown = new LinkedHashMap<>();
        Map<String, Long> failureReasons = new LinkedHashMap<>();
        for (MessageTask record : records) {
            channelBreakdown.merge(record.channel(), 1L, Long::sum);
            if (record.errorCode() != null) {
                failureReasons.merge(record.errorCode(), 1L, Long::sum);
            }
        }
        return new MessageStatisticsResponse(
                total,
                successful,
                failed,
                scheduled,
                successRate,
                channelBreakdown,
                failureReasons
        );
    }

    private VariableDefinitionResponse toVariableResponse(MessageVariable variable) {
        return new VariableDefinitionResponse(
                variable.variableCode(),
                variable.variableName(),
                variable.description(),
                variable.dataType(),
                variable.defaultValue(),
                variable.required(),
                variable.enabled(),
                variable.autoFill()
        );
    }

    private InboxMessageResponse toInboxResponse(InboxMessage inboxMessage) {
        return new InboxMessageResponse(
                inboxMessage.inboxId(),
                inboxMessage.messageId(),
                inboxMessage.receiver(),
                inboxMessage.channel(),
                inboxMessage.subject(),
                inboxMessage.content(),
                inboxMessage.readStatus(),
                inboxMessage.deliveredAt(),
                inboxMessage.readAt()
        );
    }

    private MessageResponse toResponse(MessageTask task) {
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
}
