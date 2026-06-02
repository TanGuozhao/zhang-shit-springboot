package com.example.platform.topbiz.service;

import com.example.platform.topbiz.dto.PlatformOverviewResponse;
import com.example.platform.topbiz.remote.LogServiceClient;
import com.example.platform.topbiz.remote.MessageServiceClient;
import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.remote.dto.RemoteUserProfileResponse;
import com.example.platform.topbiz.security.TopbizPrincipal;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PlatformOrchestrationService {

    private final UserServiceClient userServiceClient;
    private final MessageServiceClient messageServiceClient;
    private final LogServiceClient logServiceClient;

    public PlatformOrchestrationService(UserServiceClient userServiceClient,
                                        MessageServiceClient messageServiceClient,
                                        LogServiceClient logServiceClient) {
        this.userServiceClient = userServiceClient;
        this.messageServiceClient = messageServiceClient;
        this.logServiceClient = logServiceClient;
    }

    public PlatformOverviewResponse overview() {
        TopbizPrincipal principal = (TopbizPrincipal) SecurityUtils.getSubject().getPrincipal();
        RemoteUserProfileResponse currentUser = userServiceClient.getUser(principal.userId()).data();
        return new PlatformOverviewResponse(
                principal.userId(),
                principal.userName(),
                Map.of(
                        "profile", currentUser,
                        "department", userServiceClient.getDepartment(currentUser.departmentId()).data()
                ),
                Map.of(
                        "templates", messageServiceClient.listTemplates(null).data(),
                        "seedMessage", messageServiceClient.getMessage("MSG-SEED-1001").data()
                ),
                Map.of(
                        "metrics", logServiceClient.metrics("topbiz").data(),
                        "alerts", logServiceClient.alerts().data()
                )
        );
    }
}
