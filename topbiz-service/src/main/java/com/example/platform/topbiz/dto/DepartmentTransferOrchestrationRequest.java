package com.example.platform.topbiz.dto;

import com.example.platform.user.dto.DepartmentTransferRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DepartmentTransferOrchestrationRequest(
        @Valid @NotNull(message = "transfer is required") DepartmentTransferRequest transfer,
        String notificationTemplateCode,
        String notificationChannel,
        Map<String, String> notificationVariables,
        Boolean notifyMembers
) {
}
