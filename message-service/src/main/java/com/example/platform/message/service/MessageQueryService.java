package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.domain.MessageTask;
import com.example.platform.message.domain.MessageTemplate;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageStatusResponse;
import com.example.platform.message.dto.TemplateDetailResponse;
import com.example.platform.message.dto.TemplateSummaryResponse;
import com.example.platform.message.repository.MessageTaskRepository;
import com.example.platform.message.repository.TemplateRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MessageQueryService {

    private final MessageTaskRepository messageTaskRepository;
    private final TemplateRepository templateRepository;

    public MessageQueryService(MessageTaskRepository messageTaskRepository, TemplateRepository templateRepository) {
        this.messageTaskRepository = messageTaskRepository;
        this.templateRepository = templateRepository;
    }

    public MessageResponse getMessage(String messageId) {
        MessageTask task = messageTaskRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found"));
        return new MessageResponse(
                task.messageId(),
                task.templateCode(),
                task.channel(),
                task.status(),
                task.receivers(),
                task.variables(),
                task.createdAt()
        );
    }

    public MessageStatusResponse getStatus(String messageId) {
        MessageTask task = messageTaskRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "message not found"));
        return new MessageStatusResponse(task.messageId(), task.status(), Instant.now());
    }

    public List<TemplateSummaryResponse> listTemplates(String channel) {
        return templateRepository.findAll().stream()
                .filter(template -> channel == null || template.channel().equalsIgnoreCase(channel))
                .map(template -> new TemplateSummaryResponse(
                        template.templateCode(),
                        template.templateName(),
                        template.channel(),
                        template.enabled()
                ))
                .toList();
    }

    public TemplateDetailResponse getTemplate(String templateCode) {
        MessageTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "template not found"));
        return new TemplateDetailResponse(
                template.templateCode(),
                template.templateName(),
                template.channel(),
                template.content(),
                template.variables()
        );
    }
}
