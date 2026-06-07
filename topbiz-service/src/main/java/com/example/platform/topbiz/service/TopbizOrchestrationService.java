package com.example.platform.topbiz.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.topbiz.domain.OrchestrationExecutionRecord;
import com.example.platform.topbiz.domain.OrchestrationStepRecord;
import com.example.platform.topbiz.dto.DepartmentTransferOrchestrationRequest;
import com.example.platform.topbiz.dto.MessageAuditOrchestrationRequest;
import com.example.platform.topbiz.dto.OrchestrationExecutionResponse;
import com.example.platform.topbiz.dto.UserProvisioningOrchestrationRequest;
import com.example.platform.topbiz.repository.TopbizOrchestrationRepository;
import com.example.platform.topbiz.security.TopbizPrincipal;
import com.example.platform.user.dto.DepartmentMembershipResponse;
import com.example.platform.user.dto.DepartmentTransferRequest;
import com.example.platform.user.dto.UserProfileResponse;
import com.example.platform.user.dto.UserStatusUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TopbizOrchestrationService {

    private final TopbizUserGatewayService topbizUserGatewayService;
    private final TopbizMessageGatewayService topbizMessageGatewayService;
    private final TopbizAuditService topbizAuditService;
    private final TopbizTraceSupport topbizTraceSupport;
    private final TopbizCurrentSubjectService topbizCurrentSubjectService;
    private final TopbizOrchestrationRepository topbizOrchestrationRepository;

    public TopbizOrchestrationService(TopbizUserGatewayService topbizUserGatewayService,
                                      TopbizMessageGatewayService topbizMessageGatewayService,
                                      TopbizAuditService topbizAuditService,
                                      TopbizTraceSupport topbizTraceSupport,
                                      TopbizCurrentSubjectService topbizCurrentSubjectService,
                                      TopbizOrchestrationRepository topbizOrchestrationRepository) {
        this.topbizUserGatewayService = topbizUserGatewayService;
        this.topbizMessageGatewayService = topbizMessageGatewayService;
        this.topbizAuditService = topbizAuditService;
        this.topbizTraceSupport = topbizTraceSupport;
        this.topbizCurrentSubjectService = topbizCurrentSubjectService;
        this.topbizOrchestrationRepository = topbizOrchestrationRepository;
    }

    public OrchestrationExecutionResponse executeUserProvisioning(UserProvisioningOrchestrationRequest request) {
        String orchestrationId = "ORC-USER-" + UUID.randomUUID();
        String traceId = topbizTraceSupport.currentTraceId();
        Instant startedAt = Instant.now();
        List<OrchestrationStepRecord> steps = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();
        TopbizPrincipal principal = topbizCurrentSubjectService.requirePrincipal();

        String status = "SUCCESS";
        String errorCode = null;
        String errorMessage = null;
        UserProfileResponse createdUser = null;

        try {
            createdUser = executeCreateUserStep(request, steps);
            result.put("user", createdUser);

            if (hasWelcomeConfig(request)) {
                try {
                    MessageResponse welcomeMessage = executeWelcomeMessageStep(request, createdUser, steps);
                    result.put("welcomeMessage", welcomeMessage);
                } catch (Exception ex) {
                    errorCode = resolveCode(ex);
                    errorMessage = ex.getMessage();
                    boolean compensated = compensateDisableUser(createdUser.userId(), steps, ex.getMessage());
                    status = compensated ? "COMPENSATED" : "FAILED";
                }
            } else {
                steps.add(step("WELCOME_MESSAGE", "Send Welcome Message", "SKIPPED",
                        "welcome message not configured", null, null, Map.of()));
            }

            boolean auditLogged = topbizAuditService.audit(
                    "INFO",
                    "user provisioning orchestration executed",
                    orchestrationTags(principal, Map.of(
                            "orchestrationId", orchestrationId,
                            "status", status,
                            "userId", createdUser.userId().toString()
                    ))
            );
            steps.add(step("AUDIT_LOG", "Audit Orchestration", auditLogged ? "SUCCESS" : "WARNING",
                    auditLogged ? "audit log ingested" : "audit log ingestion failed",
                    null, null, Map.of("auditLogged", auditLogged)));
            result.put("auditLogged", auditLogged);
        } catch (Exception ex) {
            status = "FAILED";
            errorCode = resolveCode(ex);
            errorMessage = ex.getMessage();
            steps.add(step("ORCHESTRATION_ERROR", "Finalize Orchestration", "FAILED", ex.getMessage(), null, null, Map.of()));
        }

        return saveExecution(orchestrationId, "USER_PROVISIONING", resolveBusinessKey(createdUser),
                status, traceId, startedAt, steps, result, errorCode, errorMessage);
    }

    public OrchestrationExecutionResponse executeDepartmentTransfer(DepartmentTransferOrchestrationRequest request) {
        String orchestrationId = "ORC-DEPT-" + UUID.randomUUID();
        String traceId = topbizTraceSupport.currentTraceId();
        Instant startedAt = Instant.now();
        List<OrchestrationStepRecord> steps = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();
        TopbizPrincipal principal = topbizCurrentSubjectService.requirePrincipal();

        String status = "SUCCESS";
        String errorCode = null;
        String errorMessage = null;

        List<DepartmentMembershipResponse> transferred = List.of();
        try {
            transferred = executeDepartmentTransferStep(request.transfer(), steps);
            result.put("transferredMemberships", transferred);

            if (Boolean.TRUE.equals(request.notifyMembers())
                    && StringUtils.hasText(request.notificationTemplateCode())
                    && StringUtils.hasText(request.notificationChannel())) {
                try {
                    MessageResponse notification = executeTransferNotificationStep(request, transferred, steps);
                    result.put("notificationMessage", notification);
                } catch (Exception ex) {
                    errorCode = resolveCode(ex);
                    errorMessage = ex.getMessage();
                    boolean compensated = compensateDepartmentTransfer(request.transfer(), steps, ex.getMessage());
                    status = compensated ? "COMPENSATED" : "FAILED";
                }
            } else {
                steps.add(step("TRANSFER_NOTIFICATION", "Send Transfer Notification", "SKIPPED",
                        "transfer notification not configured", null, null, Map.of()));
            }

            boolean auditLogged = topbizAuditService.audit(
                    "INFO",
                    "department transfer orchestration executed",
                    orchestrationTags(principal, Map.of(
                            "orchestrationId", orchestrationId,
                            "status", status,
                            "fromDepartmentId", String.valueOf(request.transfer().fromDepartmentId()),
                            "toDepartmentId", String.valueOf(request.transfer().toDepartmentId())
                    ))
            );
            steps.add(step("AUDIT_LOG", "Audit Orchestration", auditLogged ? "SUCCESS" : "WARNING",
                    auditLogged ? "audit log ingested" : "audit log ingestion failed",
                    null, null, Map.of("auditLogged", auditLogged)));
            result.put("auditLogged", auditLogged);
        } catch (Exception ex) {
            status = "FAILED";
            errorCode = resolveCode(ex);
            errorMessage = ex.getMessage();
            steps.add(step("ORCHESTRATION_ERROR", "Finalize Orchestration", "FAILED", ex.getMessage(), null, null, Map.of()));
        }

        return saveExecution(orchestrationId, "DEPARTMENT_TRANSFER",
                request.transfer().fromDepartmentId() + "->" + request.transfer().toDepartmentId(),
                status, traceId, startedAt, steps, result, errorCode, errorMessage);
    }

    public OrchestrationExecutionResponse executeMessageAudit(MessageAuditOrchestrationRequest request) {
        String orchestrationId = "ORC-MSG-" + UUID.randomUUID();
        String traceId = topbizTraceSupport.currentTraceId();
        Instant startedAt = Instant.now();
        List<OrchestrationStepRecord> steps = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();
        TopbizPrincipal principal = topbizCurrentSubjectService.requirePrincipal();

        String status = "SUCCESS";
        String errorCode = null;
        String errorMessage = null;

        try {
            Instant sendStartedAt = Instant.now();
            MessageResponse message = topbizMessageGatewayService.send(request.message());
            steps.add(step("SEND_MESSAGE", "Send Message", "SUCCESS", "message sent",
                    sendStartedAt, Instant.now(), Map.of(
                            "messageId", message.messageId(),
                            "channel", message.channel(),
                            "status", message.status()
                    )));
            result.put("message", message);

            boolean auditLogged = topbizAuditService.audit(
                    resolveAuditLevel(request.auditLevel()),
                    StringUtils.hasText(request.auditSummary()) ? request.auditSummary() : "message audit orchestration executed",
                    orchestrationTags(principal, mergeTags(request.auditTags(), Map.of(
                            "orchestrationId", orchestrationId,
                            "messageId", message.messageId(),
                            "channel", message.channel()
                    )))
            );
            steps.add(step("AUDIT_LOG", "Ingest Audit Log", auditLogged ? "SUCCESS" : "WARNING",
                    auditLogged ? "audit log ingested" : "audit log ingestion failed",
                    null, null, Map.of("auditLogged", auditLogged)));
            result.put("auditLogged", auditLogged);
            if (!auditLogged) {
                status = "PARTIAL_SUCCESS";
            }
        } catch (Exception ex) {
            status = "FAILED";
            errorCode = resolveCode(ex);
            errorMessage = ex.getMessage();
            steps.add(step("ORCHESTRATION_ERROR", "Finalize Orchestration", "FAILED", ex.getMessage(), null, null, Map.of()));
        }

        return saveExecution(orchestrationId, "MESSAGE_AUDIT", orchestrationId,
                status, traceId, startedAt, steps, result, errorCode, errorMessage);
    }

    public List<OrchestrationExecutionResponse> listExecutions(String orchestrationType, String status, Integer limit) {
        return topbizOrchestrationRepository.findAll(orchestrationType, status, limit).stream()
                .map(OrchestrationExecutionResponse::from)
                .toList();
    }

    public OrchestrationExecutionResponse getExecution(String orchestrationId) {
        return topbizOrchestrationRepository.findById(orchestrationId)
                .map(OrchestrationExecutionResponse::from)
                .orElseThrow(() -> new BusinessException(
                        "ORCHESTRATION_NOT_FOUND",
                        "orchestration not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    private UserProfileResponse executeCreateUserStep(UserProvisioningOrchestrationRequest request,
                                                      List<OrchestrationStepRecord> steps) {
        Instant startedAt = Instant.now();
        UserProfileResponse created = topbizUserGatewayService.createUser(request.user());
        steps.add(step("CREATE_USER", "Create User", "SUCCESS", "user created",
                startedAt, Instant.now(), Map.of(
                        "userId", created.userId(),
                        "account", created.account(),
                        "status", created.status()
                )));
        return created;
    }

    private MessageResponse executeWelcomeMessageStep(UserProvisioningOrchestrationRequest request,
                                                      UserProfileResponse createdUser,
                                                      List<OrchestrationStepRecord> steps) {
        Instant startedAt = Instant.now();
        MessageSendRequest messageRequest = new MessageSendRequest(
                request.welcomeTemplateCode(),
                request.welcomeChannel(),
                List.of(resolveReceiver(createdUser, request.welcomeChannel())),
                List.of(),
                mergeVariables(request.welcomeVariables(), createdUser),
                "IMMEDIATE",
                null,
                null,
                null,
                null,
                List.of(),
                Boolean.TRUE.equals(request.saveToInbox())
        );
        MessageResponse message = topbizMessageGatewayService.send(messageRequest);
        steps.add(step("WELCOME_MESSAGE", "Send Welcome Message", "SUCCESS", "welcome message sent",
                startedAt, Instant.now(), Map.of(
                        "messageId", message.messageId(),
                        "channel", message.channel()
                )));
        return message;
    }

    private List<DepartmentMembershipResponse> executeDepartmentTransferStep(DepartmentTransferRequest request,
                                                                             List<OrchestrationStepRecord> steps) {
        Instant startedAt = Instant.now();
        List<DepartmentMembershipResponse> transferred = topbizUserGatewayService.transferMembers(request);
        steps.add(step("TRANSFER_MEMBERS", "Transfer Department Members", "SUCCESS", "members transferred",
                startedAt, Instant.now(), Map.of(
                        "userCount", transferred.size(),
                        "fromDepartmentId", request.fromDepartmentId(),
                        "toDepartmentId", request.toDepartmentId()
                )));
        return transferred;
    }

    private MessageResponse executeTransferNotificationStep(DepartmentTransferOrchestrationRequest request,
                                                            List<DepartmentMembershipResponse> transferred,
                                                            List<OrchestrationStepRecord> steps) {
        Instant startedAt = Instant.now();
        LinkedHashSet<String> receivers = new LinkedHashSet<>();
        for (DepartmentMembershipResponse membership : transferred) {
            UserProfileResponse user = topbizUserGatewayService.getUser(membership.userId());
            receivers.add(resolveReceiver(user, request.notificationChannel()));
        }
        MessageSendRequest messageRequest = new MessageSendRequest(
                request.notificationTemplateCode(),
                request.notificationChannel(),
                List.copyOf(receivers),
                List.of(),
                request.notificationVariables() == null ? Map.of() : Map.copyOf(request.notificationVariables()),
                "IMMEDIATE",
                null,
                null,
                null,
                null,
                List.of(),
                Boolean.FALSE
        );
        MessageResponse message = topbizMessageGatewayService.send(messageRequest);
        steps.add(step("TRANSFER_NOTIFICATION", "Send Transfer Notification", "SUCCESS", "transfer notification sent",
                startedAt, Instant.now(), Map.of(
                        "messageId", message.messageId(),
                        "receiverCount", receivers.size()
                )));
        return message;
    }

    private boolean compensateDisableUser(Long userId,
                                          List<OrchestrationStepRecord> steps,
                                          String reason) {
        Instant startedAt = Instant.now();
        try {
            topbizUserGatewayService.updateStatus(userId, new UserStatusUpdateRequest(
                    "DISABLED",
                    "topbiz compensation: " + reason
            ));
            steps.add(step("COMPENSATE_DISABLE_USER", "Compensate User Provisioning", "COMPENSATED",
                    "created user disabled after downstream failure",
                    startedAt, Instant.now(), Map.of("userId", userId)));
            return true;
        } catch (Exception compensationEx) {
            steps.add(step("COMPENSATE_DISABLE_USER", "Compensate User Provisioning", "FAILED",
                    compensationEx.getMessage(), startedAt, Instant.now(), Map.of("userId", userId)));
            return false;
        }
    }

    private boolean compensateDepartmentTransfer(DepartmentTransferRequest request,
                                                 List<OrchestrationStepRecord> steps,
                                                 String reason) {
        Instant startedAt = Instant.now();
        try {
            topbizUserGatewayService.transferMembers(new DepartmentTransferRequest(
                    request.userIds(),
                    request.toDepartmentId(),
                    request.fromDepartmentId()
            ));
            steps.add(step("COMPENSATE_DEPARTMENT_TRANSFER", "Compensate Department Transfer", "COMPENSATED",
                    "member transfer reverted after downstream failure",
                    startedAt, Instant.now(), Map.of(
                            "userCount", request.userIds().size(),
                            "reason", reason
                    )));
            return true;
        } catch (Exception compensationEx) {
            steps.add(step("COMPENSATE_DEPARTMENT_TRANSFER", "Compensate Department Transfer", "FAILED",
                    compensationEx.getMessage(), startedAt, Instant.now(), Map.of(
                            "userCount", request.userIds().size()
                    )));
            return false;
        }
    }

    private OrchestrationExecutionResponse saveExecution(String orchestrationId,
                                                         String orchestrationType,
                                                         String businessKey,
                                                         String status,
                                                         String traceId,
                                                         Instant startedAt,
                                                         List<OrchestrationStepRecord> steps,
                                                         Map<String, Object> result,
                                                         String errorCode,
                                                         String errorMessage) {
        OrchestrationExecutionRecord record = new OrchestrationExecutionRecord(
                orchestrationId,
                orchestrationType,
                businessKey,
                status,
                traceId,
                startedAt,
                Instant.now(),
                List.copyOf(steps),
                Map.copyOf(result),
                errorCode,
                errorMessage
        );
        topbizOrchestrationRepository.save(record);
        return OrchestrationExecutionResponse.from(record);
    }

    private OrchestrationStepRecord step(String stepCode,
                                         String stepName,
                                         String status,
                                         String message,
                                         Instant startedAt,
                                         Instant finishedAt,
                                         Map<String, Object> detail) {
        Instant now = Instant.now();
        return new OrchestrationStepRecord(
                stepCode,
                stepName,
                status,
                message,
                startedAt == null ? now : startedAt,
                finishedAt == null ? now : finishedAt,
                detail == null ? Map.of() : Map.copyOf(detail)
        );
    }

    private boolean hasWelcomeConfig(UserProvisioningOrchestrationRequest request) {
        return StringUtils.hasText(request.welcomeTemplateCode()) && StringUtils.hasText(request.welcomeChannel());
    }

    private String resolveBusinessKey(UserProfileResponse userProfileResponse) {
        if (userProfileResponse == null) {
            return "unknown";
        }
        return userProfileResponse.userId() + ":" + userProfileResponse.account();
    }

    private String resolveReceiver(UserProfileResponse user, String channel) {
        String normalizedChannel = channel == null ? "" : channel.trim().toUpperCase();
        return switch (normalizedChannel) {
            case "EMAIL" -> firstNonBlank(user.email(), user.account());
            case "SMS" -> firstNonBlank(user.phone(), user.account());
            case "INBOX" -> firstNonBlank(user.account(), user.userId() == null ? null : String.valueOf(user.userId()));
            default -> firstNonBlank(user.email(), user.phone(), user.account());
        };
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        throw new BusinessException("RECEIVER_NOT_FOUND", "no receiver could be resolved for orchestration");
    }

    private Map<String, String> mergeVariables(Map<String, String> variables, UserProfileResponse user) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (variables != null) {
            merged.putAll(variables);
        }
        merged.putIfAbsent("userId", user.userId() == null ? "" : String.valueOf(user.userId()));
        merged.putIfAbsent("account", defaultText(user.account()));
        merged.putIfAbsent("userName", defaultText(user.userName()));
        merged.putIfAbsent("email", defaultText(user.email()));
        merged.putIfAbsent("phone", defaultText(user.phone()));
        merged.putIfAbsent("departmentId", user.departmentId() == null ? "" : String.valueOf(user.departmentId()));
        return Map.copyOf(merged);
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private Map<String, String> orchestrationTags(TopbizPrincipal principal, Map<String, String> extra) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("actorUserId", String.valueOf(principal.userId()));
        tags.put("actorAccount", principal.account());
        tags.put("traceId", topbizTraceSupport.currentTraceId());
        tags.putAll(extra);
        return tags;
    }

    private Map<String, String> mergeTags(Map<String, String> first, Map<String, String> second) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (first != null) {
            tags.putAll(first);
        }
        tags.putAll(second);
        return tags;
    }

    private String resolveAuditLevel(String auditLevel) {
        if (!StringUtils.hasText(auditLevel)) {
            return "INFO";
        }
        return auditLevel.trim().toUpperCase();
    }

    private String resolveCode(Exception ex) {
        if (ex instanceof BusinessException businessException) {
            return businessException.getCode();
        }
        return "ORCHESTRATION_FAILED";
    }
}
