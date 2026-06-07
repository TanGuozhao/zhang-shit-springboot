package com.example.platform.topbiz.dto;

import com.example.platform.user.dto.UserCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UserProvisioningOrchestrationRequest(
        @Valid @NotNull(message = "user is required") UserCreateRequest user,
        String welcomeTemplateCode,
        String welcomeChannel,
        Map<String, String> welcomeVariables,
        Boolean saveToInbox
) {
}
