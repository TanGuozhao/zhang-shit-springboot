package com.example.platform.topbiz;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.log.dto.AlertResponse;
import com.example.platform.log.dto.LogEntryResponse;
import com.example.platform.log.dto.MetricsResponse;
import com.example.platform.topbiz.remote.LogServiceClient;
import com.example.platform.topbiz.remote.MessageServiceClient;
import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.remote.dto.RemoteArchitectureOverviewResponse;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginRequest;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginResponse;
import com.example.platform.user.dto.DepartmentResponse;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import com.example.platform.user.dto.UserProfileResponse;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.example.platform.topbiz.security.TopbizSessionFilter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.session.store-type=none",
        "spring.data.redis.repositories.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TopbizServiceApplicationTests {

    private static final Instant FIXED_TIME = Instant.parse("2026-06-07T08:00:00Z");

    private static final List<String> ADMIN_ROLES = List.of("ADMIN");
    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "user:self:read",
            "user:self:write",
            "user:read",
            "user:write",
            "user:status",
            "role:read",
            "role:write",
            "department:read",
            "department:write",
            "department:tree:read",
            "department:attribute:read",
            "department:attribute:write",
            "department:member:read",
            "department:member:write",
            "message:send",
            "log:query",
            "topbiz:admin",
            "topbiz:platform:read",
            "topbiz:architecture:read",
            "topbiz:orchestration:write",
            "topbiz:message:admin",
            "topbiz:log:admin",
            "topbiz:runtime:operate"
    );

    private static final List<String> OPERATOR_ROLES = List.of("OPERATOR");
    private static final List<String> OPERATOR_PERMISSIONS = List.of(
            "user:self:read",
            "user:self:write",
            "message:send",
            "log:query"
    );

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TopbizSessionFilter topbizSessionFilter;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private MessageServiceClient messageServiceClient;

    @MockBean
    private LogServiceClient logServiceClient;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(topbizSessionFilter)
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void unauthenticatedOverviewShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/topbiz/platform/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void loginShouldReturnSessionAndAllowAdminOverview() throws Exception {
        stubLogin("admin", adminLoginResponse());
        stubAuthorization(1001L, ADMIN_ROLES, ADMIN_PERMISSIONS);
        stubPlatformOverviewDependencies();

        MockHttpSession session = loginAndExtractSession("admin", "admin123");

        mockMvc.perform(get("/api/topbiz/platform/overview").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.session.userId").value(1001))
                .andExpect(jsonPath("$.data.currentUser.account").value("admin"))
                .andExpect(jsonPath("$.data.currentDepartment.departmentCode").value("PLATFORM"))
                .andExpect(jsonPath("$.data.architecture.topbiz.service").value("topbiz-service"))
                .andExpect(jsonPath("$.data.runtime.topbizMetrics.serviceName").value("topbiz"))
                .andExpect(jsonPath("$.data.quickView.recentTemplates[0].templateCode").value("WELCOME_EMAIL"));
    }

    @Test
    void operatorWithoutPlatformPermissionShouldBeForbidden() throws Exception {
        stubLogin("operator", operatorLoginResponse());
        stubAuthorization(1002L, OPERATOR_ROLES, OPERATOR_PERMISSIONS);

        MockHttpSession session = loginAndExtractSession("operator", "operator123");

        mockMvc.perform(get("/api/topbiz/platform/overview").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void userProvisioningOrchestrationShouldPersistExecution() throws Exception {
        stubLogin("admin", adminLoginResponse());
        stubAuthorization(1001L, ADMIN_ROLES, ADMIN_PERMISSIONS);
        stubUserProvisioningDependencies();

        MockHttpSession session = loginAndExtractSession("admin", "admin123");

        MvcResult createResult = mockMvc.perform(post("/api/topbiz/orchestrations/user-provisioning")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "user": {
                                    "account": "new.staff@example.com",
                                    "password": "newstaff123",
                                    "userName": "New Staff",
                                    "email": "new.staff@example.com",
                                    "phone": "13700000099",
                                    "departmentId": 10,
                                    "roles": ["USER"],
                                    "permissions": []
                                  },
                                  "saveToInbox": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orchestrationType").value("USER_PROVISIONING"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.result.user.account").value("new.staff@example.com"))
                .andExpect(jsonPath("$.data.steps[0].stepCode").value("CREATE_USER"))
                .andReturn();

        String orchestrationId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.orchestrationId");

        mockMvc.perform(get("/api/topbiz/orchestrations/{orchestrationId}", orchestrationId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orchestrationId").value(orchestrationId))
                .andExpect(jsonPath("$.data.result.auditLogged").value(true))
                .andExpect(jsonPath("$.data.steps[1].stepCode").value("WELCOME_MESSAGE"))
                .andExpect(jsonPath("$.data.steps[2].stepCode").value("AUDIT_LOG"));
    }

    private MockHttpSession loginAndExtractSession(String account, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/topbiz/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "%s"
                                }
                                """.formatted(account, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.sessionKey").isNotEmpty())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void stubPlatformOverviewDependencies() {
        when(userServiceClient.me()).thenReturn(ApiResponse.ok(adminUserProfile()));
        when(userServiceClient.getDepartment(10L)).thenReturn(ApiResponse.ok(platformDepartment()));
        when(userServiceClient.architectureOverview()).thenReturn(ApiResponse.ok(
                new RemoteArchitectureOverviewResponse(
                        "user-service",
                        "identity-access",
                        List.of("auth", "rbac", "department"),
                        List.of("controller", "dto", "service", "repository", "domain"),
                        "/api/users"
                )
        ));
        when(messageServiceClient.architectureOverview()).thenReturn(ApiResponse.ok(
                new RemoteArchitectureOverviewResponse(
                        "message-service",
                        "notification-dispatch",
                        List.of("template", "dispatch", "inbox"),
                        List.of("controller", "dto", "service", "repository", "domain"),
                        "/api/messages"
                )
        ));
        when(logServiceClient.architectureOverview()).thenReturn(ApiResponse.ok(
                new RemoteArchitectureOverviewResponse(
                        "log-service",
                        "access-observability",
                        List.of("ingest", "search", "alert"),
                        List.of("controller", "dto", "service", "repository", "domain"),
                        "/api/logs"
                )
        ));
        when(messageServiceClient.listTemplates(null)).thenReturn(ApiResponse.ok(List.of(
                new com.example.platform.message.dto.TemplateSummaryResponse(
                        "WELCOME_EMAIL",
                        "Welcome Email",
                        "EMAIL",
                        "Welcome message",
                        true
                )
        )));
        when(messageServiceClient.runtime()).thenReturn(ApiResponse.ok(
                new com.example.platform.message.dto.RuntimeOverviewResponse(
                        1L,
                        0L,
                        0L,
                        5L,
                        true,
                        true,
                        FIXED_TIME,
                        FIXED_TIME
                )
        ));
        when(logServiceClient.metrics("topbiz")).thenReturn(ApiResponse.ok(
                new MetricsResponse("topbiz", Map.of("requestCount", 12, "errorCount", 0))
        ));
        when(logServiceClient.alerts()).thenReturn(ApiResponse.ok(List.of(
                new AlertResponse("ALT-001", "TOPBIZ_WARN", "WARN", "OPEN", "mock alert")
        )));
        when(logServiceClient.runtime()).thenReturn(ApiResponse.ok(
                new com.example.platform.log.dto.RuntimeOverviewResponse(
                        2,
                        128,
                        true,
                        FIXED_TIME,
                        "SUCCESS",
                        FIXED_TIME,
                        "SUCCESS",
                        FIXED_TIME,
                        "SUCCESS",
                        FIXED_TIME,
                        "SUCCESS",
                        1,
                        3,
                        0,
                        0,
                        0,
                        null
                )
        ));
    }

    private void stubUserProvisioningDependencies() {
        when(userServiceClient.createUser(any())).thenReturn(ApiResponse.ok(
                new UserProfileResponse(
                        2001L,
                        "new.staff@example.com",
                        "New Staff",
                        "new.staff@example.com",
                        "13700000099",
                        null,
                        "ENABLED",
                        "Account enabled",
                        10L,
                        List.of("USER"),
                        List.of(),
                        Map.of(),
                        FIXED_TIME,
                        FIXED_TIME
                )
        ));
        when(logServiceClient.ingest(any())).thenReturn(ApiResponse.ok(
                new LogEntryResponse(
                        "LOG-001",
                        "topbiz",
                        "trace-001",
                        "INFO",
                        "user provisioning orchestration executed",
                        FIXED_TIME
                )
        ));
    }

    private void stubAuthorization(Long userId, List<String> roles, List<String> permissions) {
        when(userServiceClient.getRoles(eq(userId))).thenReturn(ApiResponse.ok(new RoleListResponse(userId, roles)));
        when(userServiceClient.getPermissions(eq(userId))).thenReturn(ApiResponse.ok(new PermissionListResponse(userId, permissions)));
    }

    private void stubLogin(String account, RemoteAuthLoginResponse response) {
        when(userServiceClient.login(argThat((RemoteAuthLoginRequest request) ->
                request != null && account.equals(request.account())))).thenReturn(ApiResponse.ok(response));
    }

    private RemoteAuthLoginResponse adminLoginResponse() {
        return new RemoteAuthLoginResponse(
                1001L,
                "admin",
                "Platform Admin",
                ADMIN_ROLES,
                ADMIN_PERMISSIONS,
                "sess-admin-001",
                FIXED_TIME.plusSeconds(7200)
        );
    }

    private RemoteAuthLoginResponse operatorLoginResponse() {
        return new RemoteAuthLoginResponse(
                1002L,
                "operator",
                "Ops User",
                OPERATOR_ROLES,
                OPERATOR_PERMISSIONS,
                "sess-operator-001",
                FIXED_TIME.plusSeconds(7200)
        );
    }

    private UserProfileResponse adminUserProfile() {
        return new UserProfileResponse(
                1001L,
                "admin",
                "Platform Admin",
                "admin@example.com",
                "13800000000",
                null,
                "ENABLED",
                "Account enabled",
                10L,
                ADMIN_ROLES,
                ADMIN_PERMISSIONS,
                Map.of("title", "Super Administrator"),
                FIXED_TIME,
                FIXED_TIME
        );
    }

    private DepartmentResponse platformDepartment() {
        return new DepartmentResponse(
                10L,
                "PLATFORM",
                "Platform Center",
                null,
                "platform root department",
                Map.of("region", "CN"),
                FIXED_TIME,
                FIXED_TIME
        );
    }
}
