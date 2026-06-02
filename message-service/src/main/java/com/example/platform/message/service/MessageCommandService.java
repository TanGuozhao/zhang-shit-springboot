package com.example.platform.message.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.message.domain.MessageTask;
import com.example.platform.message.domain.MessageTemplate;
import com.example.platform.message.dto.MessageDraftRequest;
import com.example.platform.message.dto.MessageResponse;
import com.example.platform.message.dto.MessageSendRequest;
import com.example.platform.message.dto.TemplatePreviewRequest;
import com.example.platform.message.dto.TemplatePreviewResponse;
import com.example.platform.message.repository.MessageTaskRepository;
import com.example.platform.message.repository.TemplateRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class MessageCommandService {

    private final MessageTaskRepository messageTaskRepository;
    private final TemplateRepository templateRepository;

    public MessageCommandService(MessageTaskRepository messageTaskRepository, TemplateRepository templateRepository) {
        this.messageTaskRepository = messageTaskRepository;
        this.templateRepository = templateRepository;
    }

    public MessageResponse send(MessageSendRequest request) {
        String messageId = "MSG-" + UUID.randomUUID();
        MessageTask task = new MessageTask(
                messageId,
                request.templateCode(),
                request.channel(),
                "SENT",
                request.receivers(),
                request.variables(),
                Instant.now()
        );
        messageTaskRepository.save(task);
        return toResponse(task);
    }

    public MessageResponse saveDraft(MessageDraftRequest request) {
        String messageId = "DRF-" + UUID.randomUUID();
        MessageTask task = new MessageTask(
                messageId,
                request.templateCode(),
                "DRAFT",
                "DRAFT",
                java.util.List.of(),
                request.variables(),
                Instant.now()
        );
        messageTaskRepository.save(task);
        return toResponse(task);
    }

    public TemplatePreviewResponse preview(String templateCode, TemplatePreviewRequest request) {
        MessageTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "template not found"));
        String rendered = template.content();
        if (request.variables() != null) {
            for (var entry : request.variables().entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return new TemplatePreviewResponse(templateCode, rendered);
    }

    private MessageResponse toResponse(MessageTask task) {
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
}
