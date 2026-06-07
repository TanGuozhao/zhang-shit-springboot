package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.domain.MessageChannel;
import com.example.platform.message.domain.MessageTemplate;
import com.example.platform.message.domain.MessageVariable;
import com.example.platform.message.domain.ReceiverGroup;
import com.example.platform.message.domain.SchedulePlan;
import com.example.platform.message.domain.SchedulePolicy;
import com.example.platform.message.domain.VariableResolution;
import com.example.platform.message.dto.ReceiverGroupResolutionResponse;
import com.example.platform.message.dto.ReceiverResolutionRequest;
import com.example.platform.message.dto.ReceiverResolutionResponse;
import com.example.platform.message.repository.ChannelRepository;
import com.example.platform.message.repository.ReceiverGroupRepository;
import com.example.platform.message.repository.SchedulePolicyRepository;
import com.example.platform.message.repository.TemplateRepository;
import com.example.platform.message.repository.VariableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MessageFlowSupportService {

    private static final Pattern SMS_PATTERN = Pattern.compile("^\\+?\\d{6,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final TemplateRepository templateRepository;
    private final VariableRepository variableRepository;
    private final ChannelRepository channelRepository;
    private final ReceiverGroupRepository receiverGroupRepository;
    private final SchedulePolicyRepository schedulePolicyRepository;

    public MessageFlowSupportService(TemplateRepository templateRepository,
                                     VariableRepository variableRepository,
                                     ChannelRepository channelRepository,
                                     ReceiverGroupRepository receiverGroupRepository,
                                     SchedulePolicyRepository schedulePolicyRepository) {
        this.templateRepository = templateRepository;
        this.variableRepository = variableRepository;
        this.channelRepository = channelRepository;
        this.receiverGroupRepository = receiverGroupRepository;
        this.schedulePolicyRepository = schedulePolicyRepository;
    }

    public MessageTemplate getTemplate(String templateCode) {
        return templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "template not found", HttpStatus.NOT_FOUND));
    }

    public MessageTemplate getEnabledTemplate(String templateCode) {
        MessageTemplate template = getTemplate(templateCode);
        if (!template.enabled()) {
            throw new BusinessException("TEMPLATE_DISABLED", "template is disabled");
        }
        return template;
    }

    public MessageChannel getAvailableChannel(String channelCode) {
        MessageChannel channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new BusinessException("CHANNEL_NOT_FOUND", "channel not found", HttpStatus.NOT_FOUND));
        if (!channel.enabled()) {
            throw new BusinessException("CHANNEL_DISABLED", "channel is disabled");
        }
        if (!channel.healthy()) {
            throw new BusinessException("CHANNEL_UNHEALTHY", "channel is unhealthy");
        }
        return channel;
    }

    public ReceiverResolutionResponse resolveReceivers(ReceiverResolutionRequest request, String channelType) {
        List<String> directReceivers = request == null ? List.of() : safeList(request.receivers());
        List<String> groupCodes = request == null ? List.of() : safeList(request.receiverGroups());
        List<ReceiverGroupResolutionResponse> resolvedGroups = new ArrayList<>();
        LinkedHashSet<String> mergedReceivers = new LinkedHashSet<>();
        for (String receiver : directReceivers) {
            validateReceiver(receiver, channelType);
            mergedReceivers.add(receiver.trim());
        }
        for (String groupCode : groupCodes) {
            ReceiverGroup group = receiverGroupRepository.findByCode(groupCode)
                    .orElseThrow(() -> new BusinessException("RECEIVER_GROUP_NOT_FOUND", "receiver group not found: " + groupCode));
            if (!group.enabled()) {
                throw new BusinessException("RECEIVER_GROUP_DISABLED", "receiver group is disabled: " + groupCode);
            }
            List<String> receivers = group.receivers().stream()
                    .map(String::trim)
                    .peek(receiver -> validateReceiver(receiver, channelType))
                    .toList();
            mergedReceivers.addAll(receivers);
            resolvedGroups.add(new ReceiverGroupResolutionResponse(group.groupCode(), receivers));
        }
        if (mergedReceivers.isEmpty()) {
            throw new BusinessException("RECEIVER_REQUIRED", "receivers are required");
        }
        return new ReceiverResolutionResponse(List.copyOf(mergedReceivers), List.copyOf(resolvedGroups));
    }

    public VariableResolution resolveVariables(String templateCode, Map<String, String> variables, boolean strict) {
        return resolveVariables(getEnabledTemplate(templateCode), variables, strict);
    }

    public VariableResolution resolveVariables(MessageTemplate template, Map<String, String> variables, boolean strict) {
        Map<String, String> incoming = variables == null ? Map.of() : variables;
        Map<String, String> resolved = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (String variableCode : template.variableCodes()) {
            MessageVariable variable = variableRepository.findByCode(variableCode)
                    .orElseThrow(() -> new BusinessException("VARIABLE_NOT_FOUND", "variable not found: " + variableCode));
            String value = incoming.get(variableCode);
            if (!StringUtils.hasText(value)) {
                value = autoFillValue(variable);
            }
            if (!StringUtils.hasText(value)) {
                value = variable.defaultValue();
            }
            if (variable.required() && !StringUtils.hasText(value)) {
                errors.add("variable " + variableCode + " is required");
                continue;
            }
            if (StringUtils.hasText(value) && !matchesType(variable.dataType(), value)) {
                errors.add("variable " + variableCode + " format invalid for type " + variable.dataType());
                continue;
            }
            if (StringUtils.hasText(value)) {
                resolved.put(variableCode, value.trim());
            }
        }
        if (!strict) {
            incoming.forEach((key, value) -> {
                if (!resolved.containsKey(key) && StringUtils.hasText(value)) {
                    resolved.put(key, value.trim());
                }
            });
        }
        return new VariableResolution(
                template,
                Map.copyOf(resolved),
                List.copyOf(errors),
                render(template.subjectTemplate(), resolved),
                render(template.contentTemplate(), resolved)
        );
    }

    public SchedulePlan validateSchedule(String dispatchType,
                                         Instant scheduledAt,
                                         String cronExpression,
                                         String schedulePolicyCode) {
        String normalizedType = normalizeDispatchType(dispatchType, scheduledAt, cronExpression, schedulePolicyCode);
        if ("IMMEDIATE".equals(normalizedType)) {
            return new SchedulePlan("IMMEDIATE", null, null, "immediate dispatch");
        }
        String normalizedCron = cronExpression;
        if (StringUtils.hasText(schedulePolicyCode)) {
            SchedulePolicy policy = schedulePolicyRepository.findByCode(schedulePolicyCode)
                    .orElseThrow(() -> new BusinessException("SCHEDULE_POLICY_NOT_FOUND", "schedule policy not found"));
            if (!policy.enabled()) {
                throw new BusinessException("SCHEDULE_POLICY_DISABLED", "schedule policy is disabled");
            }
            normalizedCron = policy.cronExpression();
        }
        if (scheduledAt == null && !StringUtils.hasText(normalizedCron)) {
            throw new BusinessException("SCHEDULE_REQUIRED", "scheduledAt or cronExpression is required for scheduled dispatch");
        }
        if (scheduledAt != null && scheduledAt.isBefore(Instant.now().minusSeconds(60))) {
            throw new BusinessException("INVALID_SCHEDULE_TIME", "scheduledAt must be in the future");
        }
        if (StringUtils.hasText(normalizedCron) && !isValidCronExpression(normalizedCron)) {
            throw new BusinessException("INVALID_CRON_EXPRESSION", "cronExpression format is invalid");
        }
        return new SchedulePlan("SCHEDULED", scheduledAt, normalizedCron, "scheduled dispatch");
    }

    public boolean isTransientFailure(Map<String, String> variables, String receiver, int retryCount) {
        boolean forcedTransientFailure = "true".equalsIgnoreCase(variables.getOrDefault("simulateFailure", "false"))
                || receiver.toLowerCase().contains("fail-once");
        return forcedTransientFailure && retryCount == 0;
    }

    public boolean isPermanentFailure(Map<String, String> variables, String receiver) {
        return "true".equalsIgnoreCase(variables.getOrDefault("simulatePermanentFailure", "false"))
                || receiver.toLowerCase().contains("always-fail");
    }

    public String render(String template, Map<String, String> variables) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    public void validateReceiver(String receiver, String channelType) {
        if (!StringUtils.hasText(receiver)) {
            throw new BusinessException("INVALID_RECEIVER", "receiver is blank");
        }
        String normalizedChannelType = channelType == null ? "" : channelType.toUpperCase();
        switch (normalizedChannelType) {
            case "EMAIL" -> {
                if (!EMAIL_PATTERN.matcher(receiver).matches()) {
                    throw new BusinessException("INVALID_RECEIVER", "invalid email receiver: " + receiver);
                }
            }
            case "SMS" -> {
                if (!SMS_PATTERN.matcher(receiver).matches()) {
                    throw new BusinessException("INVALID_RECEIVER", "invalid sms receiver: " + receiver);
                }
            }
            default -> {
            }
        }
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private String autoFillValue(MessageVariable variable) {
        if (!variable.autoFill()) {
            return null;
        }
        return switch (variable.variableCode()) {
            case "currentTime" -> Instant.now().toString();
            case "userName" -> "System User";
            default -> variable.defaultValue();
        };
    }

    private boolean matchesType(String dataType, String value) {
        try {
            return switch (dataType.toUpperCase()) {
                case "TEXT" -> true;
                case "NUMBER" -> {
                    Double.parseDouble(value);
                    yield true;
                }
                case "BOOLEAN" -> "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
                case "DATETIME" -> {
                    Instant.parse(value);
                    yield true;
                }
                case "DATE" -> {
                    LocalDate.parse(value);
                    yield true;
                }
                default -> true;
            };
        } catch (NumberFormatException | DateTimeParseException ex) {
            return false;
        }
    }

    private String normalizeDispatchType(String dispatchType, Instant scheduledAt, String cronExpression, String schedulePolicyCode) {
        if (StringUtils.hasText(dispatchType)) {
            return dispatchType.trim().toUpperCase();
        }
        if (scheduledAt != null || StringUtils.hasText(cronExpression) || StringUtils.hasText(schedulePolicyCode)) {
            return "SCHEDULED";
        }
        return "IMMEDIATE";
    }

    private boolean isValidCronExpression(String cronExpression) {
        int parts = cronExpression.trim().split("\\s+").length;
        return parts == 5 || parts == 6;
    }
}
