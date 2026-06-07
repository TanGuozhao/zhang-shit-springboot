package com.example.platform.topbiz.service;

import com.example.platform.message.dto.CarrierAccountRequest;
import com.example.platform.message.dto.CarrierAccountResponse;
import com.example.platform.message.dto.ChannelConfigRequest;
import com.example.platform.message.dto.ChannelResponse;
import com.example.platform.message.dto.ChannelSenderUpdateRequest;
import com.example.platform.message.dto.DispatchTaskListResponse;
import com.example.platform.message.dto.DispatchTaskRequest;
import com.example.platform.message.dto.DispatchTaskResponse;
import com.example.platform.message.dto.DispatchTaskTimeUpdateRequest;
import com.example.platform.message.dto.InboxListResponse;
import com.example.platform.message.dto.InboxMessageResponse;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.MessageErrorResponse;
import com.example.platform.message.dto.MessageRecordResponse;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.dto.MessageStatisticsResponse;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.ReceiverResolutionRequest;
import com.example.platform.message.dto.ReceiverResolutionResponse;
import com.example.platform.message.dto.RuntimeOverviewResponse;
import com.example.platform.message.dto.SchedulePolicyRequest;
import com.example.platform.message.dto.SchedulePolicyResponse;
import com.example.platform.message.dto.ScheduleValidationRequest;
import com.example.platform.message.dto.ScheduleValidationResponse;
import com.example.platform.message.dto.TaskExecutionResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplatePreviewRequest;
import com.example.platform.message.dto.TemplatePreviewResponse;
import com.example.platform.message.dto.TemplateStatusUpdateRequest;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.dto.TemplateUpsertRequest;
import com.example.platform.message.dto.VariableDefinitionResponse;
import com.example.platform.message.dto.VariableFillRequest;
import com.example.platform.message.dto.VariableFillResponse;
import com.example.platform.message.dto.VariableRequiredUpdateRequest;
import com.example.platform.message.dto.VariableTypeUpdateRequest;
import com.example.platform.message.dto.VariableUpsertRequest;
import com.example.platform.message.dto.VariableValidationRequest;
import com.example.platform.message.dto.VariableValidationResponse;
import com.example.platform.topbiz.remote.MessageServiceClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TopbizMessageGatewayService {

    private final MessageServiceClient messageServiceClient;
    private final RemoteCallSupport remoteCallSupport;

    public TopbizMessageGatewayService(MessageServiceClient messageServiceClient,
                                       RemoteCallSupport remoteCallSupport) {
        this.messageServiceClient = messageServiceClient;
        this.remoteCallSupport = remoteCallSupport;
    }

    public MessageResponse send(MessageSendRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.send(request));
    }

    public MessageResponse saveDraft(MessageDraftRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.saveDraft(request));
    }

    public TemplatePreviewResponse preview(String templateCode, TemplatePreviewRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.preview(templateCode, request));
    }

    public VariableFillResponse fillVariables(VariableFillRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.fillVariables(request));
    }

    public VariableValidationResponse validateVariables(VariableValidationRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.validateVariables(request));
    }

    public ScheduleValidationResponse validateSchedule(ScheduleValidationRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.validateSchedule(request));
    }

    public ReceiverResolutionResponse resolveReceivers(ReceiverResolutionRequest request, String channel) {
        return remoteCallSupport.unwrap(messageServiceClient.resolveReceivers(request, channel));
    }

    public InboxMessageResponse markRead(String inboxId) {
        return remoteCallSupport.unwrap(messageServiceClient.markRead(inboxId));
    }

    public MessageResponse getMessage(String messageId) {
        return remoteCallSupport.unwrap(messageServiceClient.getMessage(messageId));
    }

    public MessageStatusResponse getStatus(String messageId) {
        return remoteCallSupport.unwrap(messageServiceClient.getStatus(messageId));
    }

    public MessageErrorResponse getError(String messageId) {
        return remoteCallSupport.unwrap(messageServiceClient.getError(messageId));
    }

    public List<TemplateSummaryResponse> listTemplates(String channel) {
        return remoteCallSupport.unwrap(messageServiceClient.listTemplates(channel));
    }

    public TemplateDetailResponse getTemplate(String templateCode) {
        return remoteCallSupport.unwrap(messageServiceClient.getTemplate(templateCode));
    }

    public List<VariableDefinitionResponse> getTemplateVariables(String templateCode) {
        return remoteCallSupport.unwrap(messageServiceClient.getTemplateVariables(templateCode));
    }

    public List<ChannelResponse> listChannels() {
        return remoteCallSupport.unwrap(messageServiceClient.listChannels());
    }

    public MessageRecordResponse listRecords(String status,
                                             String channel,
                                             String keyword,
                                             String receiver,
                                             Instant startTime,
                                             Instant endTime) {
        return remoteCallSupport.unwrap(messageServiceClient.listRecords(
                status, channel, keyword, receiver, startTime, endTime
        ));
    }

    public MessageRecordResponse search(String keyword) {
        return remoteCallSupport.unwrap(messageServiceClient.search(keyword));
    }

    public InboxListResponse listInbox(String receiver) {
        return remoteCallSupport.unwrap(messageServiceClient.listInbox(receiver));
    }

    public InboxMessageResponse getInboxMessage(String inboxId) {
        return remoteCallSupport.unwrap(messageServiceClient.getInboxMessage(inboxId));
    }

    public TemplateDetailResponse upsertTemplate(TemplateUpsertRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.upsertTemplate(request));
    }

    public TemplateDetailResponse updateTemplateStatus(String templateCode, TemplateStatusUpdateRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.updateTemplateStatus(templateCode, request));
    }

    public VariableDefinitionResponse upsertVariable(VariableUpsertRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.upsertVariable(request));
    }

    public VariableDefinitionResponse updateVariableType(String variableCode, VariableTypeUpdateRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.updateVariableType(variableCode, request));
    }

    public VariableDefinitionResponse updateVariableRequired(String variableCode, VariableRequiredUpdateRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.updateVariableRequired(variableCode, request));
    }

    public CarrierAccountResponse createCarrierAccount(CarrierAccountRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.createCarrierAccount(request));
    }

    public ChannelResponse configureChannel(ChannelConfigRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.configureChannel(request));
    }

    public ChannelResponse updateChannelSender(String channelCode, ChannelSenderUpdateRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.updateChannelSender(channelCode, request));
    }

    public SchedulePolicyResponse createSchedulePolicy(SchedulePolicyRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.createSchedulePolicy(request));
    }

    public DispatchTaskResponse createDispatchTask(DispatchTaskRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.createDispatchTask(request));
    }

    public DispatchTaskListResponse listDispatchTasks(String status, Instant startTime, Instant endTime) {
        return remoteCallSupport.unwrap(messageServiceClient.listDispatchTasks(status, startTime, endTime));
    }

    public DispatchTaskResponse updateDispatchTaskTime(String taskId, DispatchTaskTimeUpdateRequest request) {
        return remoteCallSupport.unwrap(messageServiceClient.updateDispatchTaskTime(taskId, request));
    }

    public DispatchTaskResponse cancelDispatchTask(String taskId) {
        return remoteCallSupport.unwrap(messageServiceClient.cancelDispatchTask(taskId));
    }

    public DispatchTaskResponse triggerDispatchTask(String taskId) {
        return remoteCallSupport.unwrap(messageServiceClient.triggerDispatchTask(taskId));
    }

    public TaskExecutionResponse runRetries() {
        return remoteCallSupport.unwrap(messageServiceClient.runRetries());
    }

    public MessageStatisticsResponse getStatistics(Instant startTime, Instant endTime) {
        return remoteCallSupport.unwrap(messageServiceClient.getStatistics(startTime, endTime));
    }

    public RuntimeOverviewResponse runtime() {
        return remoteCallSupport.unwrap(messageServiceClient.runtime());
    }

    public TaskExecutionResponse runDispatchTasks() {
        return remoteCallSupport.unwrap(messageServiceClient.runDispatchTasks());
    }

    public TaskExecutionResponse runRetryTasks() {
        return remoteCallSupport.unwrap(messageServiceClient.runRetryTasks());
    }
}
