package com.example.platform.topbiz.service;

import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.topbiz.dto.LoginResponse;
import com.example.platform.topbiz.dto.OrchestrationExecutionResponse;
import com.example.platform.topbiz.dto.PlatformArchitectureResponse;
import com.example.platform.topbiz.dto.PlatformOverviewResponse;
import com.example.platform.topbiz.dto.PlatformRuntimeResponse;
import com.example.platform.topbiz.remote.LogServiceClient;
import com.example.platform.topbiz.remote.MessageServiceClient;
import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.remote.dto.RemoteArchitectureOverviewResponse;
import com.example.platform.topbiz.repository.TopbizOrchestrationRepository;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformOrchestrationService {

    private final TopbizAuthenticationService topbizAuthenticationService;
    private final TopbizUserGatewayService topbizUserGatewayService;
    private final TopbizMessageGatewayService topbizMessageGatewayService;
    private final TopbizLogGatewayService topbizLogGatewayService;
    private final UserServiceClient userServiceClient;
    private final MessageServiceClient messageServiceClient;
    private final LogServiceClient logServiceClient;
    private final RemoteCallSupport remoteCallSupport;
    private final TopbizOrchestrationRepository topbizOrchestrationRepository;

    public PlatformOrchestrationService(TopbizAuthenticationService topbizAuthenticationService,
                                        TopbizUserGatewayService topbizUserGatewayService,
                                        TopbizMessageGatewayService topbizMessageGatewayService,
                                        TopbizLogGatewayService topbizLogGatewayService,
                                        UserServiceClient userServiceClient,
                                        MessageServiceClient messageServiceClient,
                                        LogServiceClient logServiceClient,
                                        RemoteCallSupport remoteCallSupport,
                                        TopbizOrchestrationRepository topbizOrchestrationRepository) {
        this.topbizAuthenticationService = topbizAuthenticationService;
        this.topbizUserGatewayService = topbizUserGatewayService;
        this.topbizMessageGatewayService = topbizMessageGatewayService;
        this.topbizLogGatewayService = topbizLogGatewayService;
        this.userServiceClient = userServiceClient;
        this.messageServiceClient = messageServiceClient;
        this.logServiceClient = logServiceClient;
        this.remoteCallSupport = remoteCallSupport;
        this.topbizOrchestrationRepository = topbizOrchestrationRepository;
    }

    public PlatformOverviewResponse overview() {
        LoginResponse session = topbizAuthenticationService.currentSession();
        UserProfileResponse currentUser = topbizUserGatewayService.currentUser();
        DepartmentResponse currentDepartment = topbizUserGatewayService.getDepartment(currentUser.departmentId());
        PlatformArchitectureResponse architecture = architecture();
        PlatformRuntimeResponse runtime = runtime();

        List<OrchestrationExecutionResponse> recentOrchestrations = topbizOrchestrationRepository
                .findAll(null, null, 5)
                .stream()
                .map(OrchestrationExecutionResponse::from)
                .toList();

        Map<String, Object> quickView = new LinkedHashMap<>();
        quickView.put("recentTemplates", topbizMessageGatewayService.listTemplates(null));
        quickView.put("recentAlerts", runtime.alerts());
        quickView.put("recentOrchestrations", recentOrchestrations);
        quickView.put("currentPermissions", currentUser.permissions());

        return new PlatformOverviewResponse(
                session,
                currentUser,
                currentDepartment,
                architecture,
                runtime,
                quickView
        );
    }

    public PlatformArchitectureResponse architecture() {
        return new PlatformArchitectureResponse(
                topbizArchitectureOverview(),
                remoteCallSupport.unwrap(userServiceClient.architectureOverview()),
                remoteCallSupport.unwrap(messageServiceClient.architectureOverview()),
                remoteCallSupport.unwrap(logServiceClient.architectureOverview())
        );
    }

    public PlatformRuntimeResponse runtime() {
        MetricsResponse metrics = topbizLogGatewayService.metrics("topbiz");
        List<AlertResponse> alerts = topbizLogGatewayService.alerts();
        return new PlatformRuntimeResponse(
                metrics,
                topbizMessageGatewayService.runtime(),
                topbizLogGatewayService.runtime(),
                alerts
        );
    }

    private RemoteArchitectureOverviewResponse topbizArchitectureOverview() {
        return new RemoteArchitectureOverviewResponse(
                "topbiz-service",
                "gateway-orchestration-auth",
                List.of("auth", "gateway", "orchestration", "audit", "platform"),
                List.of("controller", "dto", "service", "repository", "config", "security", "remote", "domain"),
                "/api/topbiz"
        );
    }
}
