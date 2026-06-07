package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.domain.CarrierAccount;
import com.example.platform.message.domain.DispatchTask;
import com.example.platform.message.domain.MessageChannel;
import com.example.platform.message.domain.MessageTemplate;
import com.example.platform.message.domain.MessageVariable;
import com.example.platform.message.domain.SchedulePolicy;
import com.example.platform.message.dto.CarrierAccountRequest;
import com.example.platform.message.dto.CarrierAccountResponse;
import com.example.platform.message.dto.ChannelConfigRequest;
import com.example.platform.message.dto.ChannelResponse;
import com.example.platform.message.dto.ChannelSenderUpdateRequest;
import com.example.platform.message.dto.DispatchTaskListResponse;
import com.example.platform.message.dto.DispatchTaskRequest;
import com.example.platform.message.dto.DispatchTaskResponse;
import com.example.platform.message.dto.DispatchTaskTimeUpdateRequest;
import com.example.platform.message.dto.MessageStatisticsResponse;
import com.example.platform.message.dto.SchedulePolicyRequest;
import com.example.platform.message.dto.SchedulePolicyResponse;
import com.example.platform.message.dto.TaskExecutionResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplateStatusUpdateRequest;
import com.example.platform.message.dto.TemplateUpsertRequest;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.dto.VariableRequiredUpdateRequest;
import com.example.platform.message.dto.VariableTypeUpdateRequest;
import com.example.platform.message.dto.VariableUpsertRequest;
import com.example.platform.message.repository.CarrierAccountRepository;
import com.example.platform.message.repository.ChannelRepository;
import com.example.platform.message.repository.DispatchTaskRepository;
import com.example.platform.message.repository.MessageTaskRepository;
import com.example.platform.message.repository.RetryRecordRepository;
import com.example.platform.message.repository.SchedulePolicyRepository;
import com.example.platform.message.repository.TemplateRepository;
import com.example.platform.message.repository.VariableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MessageAdminService {

    private final TemplateRepository templateRepository;
    private final VariableRepository variableRepository;
    private final CarrierAccountRepository carrierAccountRepository;
    private final ChannelRepository channelRepository;
    private final SchedulePolicyRepository schedulePolicyRepository;
    private final DispatchTaskRepository dispatchTaskRepository;
    private final MessageTaskRepository messageTaskRepository;
    private final RetryRecordRepository retryRecordRepository;
    private final MessageQueryService messageQueryService;
    private final MessageOperationsService messageOperationsService;

    public MessageAdminService(TemplateRepository templateRepository,
                               VariableRepository variableRepository,
                               CarrierAccountRepository carrierAccountRepository,
                               ChannelRepository channelRepository,
                               SchedulePolicyRepository schedulePolicyRepository,
                               DispatchTaskRepository dispatchTaskRepository,
                               MessageTaskRepository messageTaskRepository,
                               RetryRecordRepository retryRecordRepository,
                               MessageQueryService messageQueryService,
                               MessageOperationsService messageOperationsService) {
        this.templateRepository = templateRepository;
        this.variableRepository = variableRepository;
        this.carrierAccountRepository = carrierAccountRepository;
        this.channelRepository = channelRepository;
        this.schedulePolicyRepository = schedulePolicyRepository;
        this.dispatchTaskRepository = dispatchTaskRepository;
        this.messageTaskRepository = messageTaskRepository;
        this.retryRecordRepository = retryRecordRepository;
        this.messageQueryService = messageQueryService;
        this.messageOperationsService = messageOperationsService;
    }

    public TemplateDetailResponse upsertTemplate(TemplateUpsertRequest request) {
        validateTemplateRequest(request);
        MessageTemplate template = new MessageTemplate(
                request.templateCode(),
                request.templateName(),
                request.channel(),
                request.subjectTemplate(),
                request.contentTemplate(),
                request.description(),
                request.enabled() == null || request.enabled(),
                request.variableCodes()
        );
        templateRepository.save(template);
        return messageQueryService.getTemplate(template.templateCode());
    }

    public TemplateDetailResponse updateTemplateStatus(String templateCode, TemplateStatusUpdateRequest request) {
        MessageTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "template not found", HttpStatus.NOT_FOUND));
        MessageTemplate updated = new MessageTemplate(
                template.templateCode(),
                template.templateName(),
                template.channel(),
                template.subjectTemplate(),
                template.contentTemplate(),
                template.description(),
                request.enabled(),
                template.variableCodes()
        );
        templateRepository.save(updated);
        return messageQueryService.getTemplate(templateCode);
    }

    public VariableDefinitionResponse upsertVariable(VariableUpsertRequest request) {
        validateVariableType(request.dataType());
        MessageVariable variable = new MessageVariable(
                request.variableCode(),
                request.variableName(),
                request.description(),
                request.dataType(),
                request.defaultValue(),
                request.required(),
                request.enabled() == null || request.enabled(),
                request.autoFill() != null && request.autoFill()
        );
        variableRepository.save(variable);
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

    public VariableDefinitionResponse updateVariableType(String variableCode, VariableTypeUpdateRequest request) {
        validateVariableType(request.dataType());
        MessageVariable variable = requireVariable(variableCode);
        MessageVariable updated = new MessageVariable(
                variable.variableCode(),
                variable.variableName(),
                variable.description(),
                request.dataType(),
                variable.defaultValue(),
                variable.required(),
                variable.enabled(),
                variable.autoFill()
        );
        variableRepository.save(updated);
        return toVariableResponse(updated);
    }

    public VariableDefinitionResponse updateVariableRequired(String variableCode, VariableRequiredUpdateRequest request) {
        MessageVariable variable = requireVariable(variableCode);
        MessageVariable updated = new MessageVariable(
                variable.variableCode(),
                variable.variableName(),
                variable.description(),
                variable.dataType(),
                variable.defaultValue(),
                request.required(),
                variable.enabled(),
                variable.autoFill()
        );
        variableRepository.save(updated);
        return toVariableResponse(updated);
    }

    public CarrierAccountResponse createCarrierAccount(CarrierAccountRequest request) {
        validateCarrierAccountRequest(request);
        CarrierAccount account = new CarrierAccount(
                request.accountCode(),
                request.carrierName(),
                request.channelType(),
                request.apiKey(),
                request.endpoint(),
                request.signature(),
                request.enabled() == null || request.enabled()
        );
        carrierAccountRepository.save(account);
        return new CarrierAccountResponse(
                account.accountCode(),
                account.carrierName(),
                account.channelType(),
                account.endpoint(),
                account.signature(),
                account.enabled()
        );
    }

    public ChannelResponse configureChannel(ChannelConfigRequest request) {
        validateChannelConfig(request);
        MessageChannel channel = new MessageChannel(
                request.channelCode(),
                request.channelType(),
                request.carrierName(),
                request.accountCode(),
                request.sender(),
                request.enabled() == null || request.enabled(),
                request.healthy() == null || request.healthy(),
                request.description()
        );
        channelRepository.save(channel);
        return toChannelResponse(channel);
    }

    public ChannelResponse updateChannelSender(String channelCode, ChannelSenderUpdateRequest request) {
        MessageChannel channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new BusinessException("CHANNEL_NOT_FOUND", "channel not found", HttpStatus.NOT_FOUND));
        MessageChannel updated = new MessageChannel(
                channel.channelCode(),
                channel.channelType(),
                channel.carrierName(),
                channel.accountCode(),
                request.sender(),
                channel.enabled(),
                channel.healthy(),
                channel.description()
        );
        channelRepository.save(updated);
        return toChannelResponse(updated);
    }

    public SchedulePolicyResponse createSchedulePolicy(SchedulePolicyRequest request) {
        validateSchedulePolicy(request);
        Instant now = Instant.now();
        SchedulePolicy policy = new SchedulePolicy(
                request.policyCode(),
                request.cronExpression(),
                request.policyType(),
                request.enabled() == null || request.enabled(),
                request.description(),
                now,
                now
        );
        schedulePolicyRepository.save(policy);
        return new SchedulePolicyResponse(
                policy.policyCode(),
                policy.cronExpression(),
                policy.policyType(),
                policy.enabled(),
                policy.description()
        );
    }

    public DispatchTaskResponse createDispatchTask(DispatchTaskRequest request) {
        validateDispatchTaskRequest(request);
        messageTaskRepository.findById(request.messageId())
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found", HttpStatus.NOT_FOUND));
        Instant now = Instant.now();
        DispatchTask task = new DispatchTask(
                "DST-" + UUID.randomUUID(),
                request.taskCode(),
                request.messageId(),
                messageTaskRepository.findById(request.messageId()).orElseThrow().receivers().stream().findFirst().orElse("unknown"),
                request.channelCode(),
                request.channelAccountCode(),
                request.schedulePolicyCode(),
                null,
                request.plannedAt(),
                null,
                "PENDING",
                request.sortOrder() == null ? 1 : request.sortOrder(),
                null,
                now,
                now
        );
        dispatchTaskRepository.save(task);
        return toDispatchTaskResponse(task);
    }

    public DispatchTaskListResponse listDispatchTasks(String status, Instant startTime, Instant endTime) {
        List<DispatchTaskResponse> records = dispatchTaskRepository.search(status, startTime, endTime).stream()
                .map(this::toDispatchTaskResponse)
                .toList();
        return new DispatchTaskListResponse(records, records.size());
    }

    public DispatchTaskResponse updateDispatchTaskTime(String taskId, DispatchTaskTimeUpdateRequest request) {
        DispatchTask task = requireDispatchTask(taskId);
        DispatchTask updated = new DispatchTask(
                task.taskId(),
                task.taskCode(),
                task.messageId(),
                task.receiver(),
                task.channelCode(),
                task.channelAccountCode(),
                task.schedulePolicyCode(),
                task.batchCode(),
                request.plannedAt(),
                task.actualAt(),
                task.status(),
                task.sortOrder(),
                task.lastError(),
                task.createdAt(),
                Instant.now()
        );
        dispatchTaskRepository.save(updated);
        return toDispatchTaskResponse(updated);
    }

    public DispatchTaskResponse cancelDispatchTask(String taskId) {
        DispatchTask task = requireDispatchTask(taskId);
        DispatchTask updated = new DispatchTask(
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
                "CANCELLED",
                task.sortOrder(),
                task.lastError(),
                task.createdAt(),
                Instant.now()
        );
        dispatchTaskRepository.save(updated);
        retryRecordRepository.cancelPendingByDispatchTaskId(taskId, "dispatch task cancelled by admin");
        messageOperationsService.synchronizeMessageState(task.messageId(), task.batchCode());
        return toDispatchTaskResponse(updated);
    }

    public DispatchTaskResponse triggerDispatchTask(String taskId) {
        messageOperationsService.runDispatchTask(taskId);
        return toDispatchTaskResponse(requireDispatchTask(taskId));
    }

    public TaskExecutionResponse runRetries() {
        return messageOperationsService.runPendingRetries();
    }

    public MessageStatisticsResponse getStatistics(Instant startTime, Instant endTime) {
        return messageQueryService.getStatistics(startTime, endTime);
    }

    private MessageVariable requireVariable(String variableCode) {
        return variableRepository.findByCode(variableCode)
                .orElseThrow(() -> new BusinessException("VARIABLE_NOT_FOUND", "variable not found", HttpStatus.NOT_FOUND));
    }

    private void validateTemplateRequest(TemplateUpsertRequest request) {
        if (!StringUtils.hasText(request.templateCode())
                || !StringUtils.hasText(request.templateName())
                || !StringUtils.hasText(request.channel())
                || !StringUtils.hasText(request.subjectTemplate())
                || !StringUtils.hasText(request.contentTemplate())) {
            throw new BusinessException("VALIDATION_ERROR", "templateCode, templateName, channel, subjectTemplate and contentTemplate are required");
        }
        if (request.variableCodes() == null) {
            throw new BusinessException("VALIDATION_ERROR", "variableCodes are required");
        }
        if (!variableRepository.findAllByCodes(request.variableCodes()).stream()
                .map(MessageVariable::variableCode)
                .toList()
                .containsAll(request.variableCodes())) {
            throw new BusinessException("VARIABLE_NOT_FOUND", "template references unknown variables");
        }
    }

    private void validateVariableType(String dataType) {
        if (!StringUtils.hasText(dataType)) {
            throw new BusinessException("VALIDATION_ERROR", "dataType is required");
        }
        List<String> supportedTypes = List.of("TEXT", "NUMBER", "DATE", "DATETIME", "BOOLEAN");
        if (!supportedTypes.contains(dataType.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("VALIDATION_ERROR", "unsupported variable data type");
        }
    }

    private void validateCarrierAccountRequest(CarrierAccountRequest request) {
        if (!StringUtils.hasText(request.accountCode())
                || !StringUtils.hasText(request.carrierName())
                || !StringUtils.hasText(request.channelType())) {
            throw new BusinessException("VALIDATION_ERROR", "accountCode, carrierName and channelType are required");
        }
    }

    private void validateChannelConfig(ChannelConfigRequest request) {
        if (!StringUtils.hasText(request.channelCode())
                || !StringUtils.hasText(request.channelType())
                || !StringUtils.hasText(request.carrierName())
                || !StringUtils.hasText(request.accountCode())
                || !StringUtils.hasText(request.sender())) {
            throw new BusinessException("VALIDATION_ERROR", "channelCode, channelType, carrierName, accountCode and sender are required");
        }
        CarrierAccount account = carrierAccountRepository.findByCode(request.accountCode())
                .orElseThrow(() -> new BusinessException("CARRIER_ACCOUNT_NOT_FOUND", "carrier account not found", HttpStatus.NOT_FOUND));
        if (!account.enabled()) {
            throw new BusinessException("CARRIER_ACCOUNT_DISABLED", "carrier account is disabled");
        }
        if (!account.channelType().equalsIgnoreCase(request.channelType())) {
            throw new BusinessException("VALIDATION_ERROR", "channel type does not match carrier account");
        }
    }

    private void validateSchedulePolicy(SchedulePolicyRequest request) {
        if (!StringUtils.hasText(request.policyCode()) || !StringUtils.hasText(request.policyType())) {
            throw new BusinessException("VALIDATION_ERROR", "policyCode and policyType are required");
        }
        String normalizedType = request.policyType().trim().toUpperCase(Locale.ROOT);
        if (!List.of("CRON", "ONCE").contains(normalizedType)) {
            throw new BusinessException("VALIDATION_ERROR", "unsupported policyType");
        }
        if ("CRON".equals(normalizedType) && !StringUtils.hasText(request.cronExpression())) {
            throw new BusinessException("VALIDATION_ERROR", "cronExpression is required for CRON policy");
        }
    }

    private void validateDispatchTaskRequest(DispatchTaskRequest request) {
        if (!StringUtils.hasText(request.taskCode())
                || !StringUtils.hasText(request.messageId())
                || !StringUtils.hasText(request.channelCode())) {
            throw new BusinessException("VALIDATION_ERROR", "taskCode, messageId and channelCode are required");
        }
        MessageChannel channel = channelRepository.findByCode(request.channelCode())
                .orElseThrow(() -> new BusinessException("CHANNEL_NOT_FOUND", "channel not found", HttpStatus.NOT_FOUND));
        if (!channel.enabled() || !channel.healthy()) {
            throw new BusinessException("CHANNEL_UNAVAILABLE", "channel is not available");
        }
        if (StringUtils.hasText(request.channelAccountCode())) {
            CarrierAccount account = carrierAccountRepository.findByCode(request.channelAccountCode())
                    .orElseThrow(() -> new BusinessException("CARRIER_ACCOUNT_NOT_FOUND", "carrier account not found", HttpStatus.NOT_FOUND));
            if (!account.enabled()) {
                throw new BusinessException("CARRIER_ACCOUNT_DISABLED", "carrier account is disabled");
            }
        }
        if (StringUtils.hasText(request.schedulePolicyCode())) {
            SchedulePolicy policy = schedulePolicyRepository.findByCode(request.schedulePolicyCode())
                    .orElseThrow(() -> new BusinessException("SCHEDULE_POLICY_NOT_FOUND", "schedule policy not found", HttpStatus.NOT_FOUND));
            if (!policy.enabled()) {
                throw new BusinessException("SCHEDULE_POLICY_DISABLED", "schedule policy is disabled");
            }
        }
    }

    private DispatchTask requireDispatchTask(String taskId) {
        return dispatchTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("DISPATCH_TASK_NOT_FOUND", "dispatch task not found", HttpStatus.NOT_FOUND));
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

    private ChannelResponse toChannelResponse(MessageChannel channel) {
        return new ChannelResponse(
                channel.channelCode(),
                channel.channelType(),
                channel.carrierName(),
                channel.accountCode(),
                channel.sender(),
                channel.enabled(),
                channel.healthy(),
                channel.description()
        );
    }

    private DispatchTaskResponse toDispatchTaskResponse(DispatchTask task) {
        return new DispatchTaskResponse(
                task.taskId(),
                task.taskCode(),
                task.messageId(),
                task.receiver(),
                task.channelCode(),
                task.plannedAt(),
                task.actualAt(),
                task.status(),
                task.sortOrder(),
                task.lastError()
        );
    }
}
