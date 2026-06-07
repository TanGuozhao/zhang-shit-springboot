package com.example.platform.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceApplicationTests {

    private static final String ADMIN_USER_ID = "1001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void loginShouldReturnSessionKey() throws Exception {
        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.sessionKey").isNotEmpty());
    }

    @Test
    void currentUserShouldBeResolvedByHeader() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", "1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1002))
                .andExpect(jsonPath("$.data.account").value("operator"));
    }

    @Test
    void getDepartmentShouldReturnCurrentUserDepartmentInfo() throws Exception {
        mockMvc.perform(get("/api/users/departments/20")
                        .header("X-User-Id", "1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentCode").value("OPS"));
    }

    @Test
    void createUserShouldPersistInMemory() throws Exception {
        mockMvc.perform(post("/api/users/admin")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "tester",
                                  "password": "tester123",
                                  "userName": "Test User",
                                  "email": "tester@example.com",
                                  "phone": "13700000000",
                                  "departmentId": 10,
                                  "roles": ["AUDITOR"],
                                  "permissions": ["user:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("tester"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    @Test
    void updateStatusShouldDisableUser() throws Exception {
        mockMvc.perform(patch("/api/users/admin/1002/status")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1002))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void updateAuthorizationShouldRefreshRolesAndPermissions() throws Exception {
        mockMvc.perform(patch("/api/users/admin/1002/authorization")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["OPS_ADMIN"],
                                  "permissions": ["message:send", "log:query", "user:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("OPS_ADMIN"));

        mockMvc.perform(post("/api/users/admin/1002/authorization/permissions:refresh")
                        .header("X-User-Id", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions[?(@ == 'user:read')]").exists())
                .andExpect(jsonPath("$.data.permissions[?(@ == 'message:send')]").exists())
                .andExpect(jsonPath("$.data.permissions[?(@ == 'log:query')]").exists())
                .andExpect(jsonPath("$.data.permissions[?(@ == 'topbiz:platform:read')]").exists())
                .andExpect(jsonPath("$.data.permissions[?(@ == 'topbiz:runtime:operate')]").exists());
    }

    @Test
    void registerAndLoginShouldFollowCorrectedContract() throws Exception {
        sendVerifyCode("newuser@example.com", "newuser@example.com", "REGISTER");

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "newuser@example.com",
                                  "password": "newuser123",
                                  "userName": "New User",
                                  "contact": "newuser@example.com",
                                  "verifyCode": "123456",
                                  "agreeProtocol": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "newuser@example.com",
                                  "password": "newuser123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("New User"))
                .andExpect(jsonPath("$.data.expireTime").isNotEmpty());
    }

    @Test
    void selfProfilePasswordAndForgotPasswordFlowShouldWork() throws Exception {
        sendVerifyCode("selfflow@example.com", "selfflow@example.com", "REGISTER");
        registerUser("selfflow@example.com", "selfflow123", "Self Flow", "selfflow@example.com");

        String sessionKey = loginAndExtractSessionKey("selfflow@example.com", "selfflow123");

        mockMvc.perform(put("/api/users/me")
                        .header("X-Session-Key", sessionKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "selfflow123",
                                  "userName": "Self Flow Updated",
                                  "contact": "selfflow-updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("Self Flow Updated"))
                .andExpect(jsonPath("$.data.email").value("selfflow-updated@example.com"));

        mockMvc.perform(get("/api/users/me/modify-records")
                        .header("X-Session-Key", sessionKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(put("/api/users/me/password")
                        .header("X-Session-Key", sessionKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "selfflow123",
                                  "newPassword": "selfflow456",
                                  "confirmPassword": "selfflow456"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/password/forgot/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "selfflow@example.com",
                                  "contact": "selfflow-updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("selfflow@example.com"));

        mockMvc.perform(post("/api/users/password/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "selfflow@example.com",
                                  "contact": "selfflow-updated@example.com",
                                  "verifyCode": "123456",
                                  "newPassword": "selfflow789",
                                  "confirmPassword": "selfflow789"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "selfflow@example.com",
                                  "password": "selfflow789"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("selfflow@example.com"));
    }

    @Test
    void loginFreezeAndUnfreezeShouldWork() throws Exception {
        sendVerifyCode("freeze@example.com", "freeze@example.com", "REGISTER");
        registerUser("freeze@example.com", "freeze123", "Frozen User", "freeze@example.com");

        for (int index = 0; index < 4; index++) {
            mockMvc.perform(post("/api/users/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "account": "freeze@example.com",
                                      "password": "wrong123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "freeze@example.com",
                                  "password": "wrong123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FROZEN"));

        sendVerifyCode("freeze@example.com", "freeze@example.com", "UNFREEZE");

        mockMvc.perform(post("/api/users/me/status/unfreeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "freeze@example.com",
                                  "contact": "freeze@example.com",
                                  "verifyCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "freeze@example.com",
                                  "password": "freeze123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("freeze@example.com"));
    }

    @Test
    void roleAndDepartmentAdminApisShouldWork() throws Exception {
        mockMvc.perform(get("/api/users/admin/roles")
                        .header("X-User-Id", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6));

        mockMvc.perform(post("/api/users/admin/roles")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCode": "CASE_ADMIN",
                                  "roleName": "Case Administrator",
                                  "description": "Case management admin",
                                  "permissions": ["user:read", "message:send"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("CASE_ADMIN"));

        mockMvc.perform(post("/api/users/admin/departments")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentCode": "QA",
                                  "departmentName": "Quality Assurance",
                                  "parentDepartmentId": 10,
                                  "description": "QA team"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentCode").value("QA"));
    }

    @Test
    void departmentTreeAttributesAndMemberOperationsShouldWork() throws Exception {
        Long userId = createAdminUser(
                "dept-member@example.com",
                "13700000011",
                20L,
                "Department Member"
        );

        MvcResult createDepartmentResult = mockMvc.perform(post("/api/users/admin/departments")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentCode": "QA",
                                  "departmentName": "Quality Assurance",
                                  "parentDepartmentId": 10,
                                  "description": "QA team"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentCode").value("QA"))
                .andReturn();
        Long departmentId = extractLong(createDepartmentResult.getResponse().getContentAsString(), "$.data.departmentId");

        mockMvc.perform(get("/api/users/admin/departments/tree")
                        .header("X-User-Id", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].departmentCode").value("PLATFORM"))
                .andExpect(jsonPath("$.data[0].children[?(@.departmentCode == 'QA')]").exists());

        mockMvc.perform(post("/api/users/admin/departments/{departmentId}/attributes/definitions", departmentId)
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attributeKey": "costCenter",
                                  "attributeName": "Cost Center",
                                  "dataType": "TEXT",
                                  "defaultValue": "CC-01",
                                  "required": true,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributeKey").value("costCenter"));

        mockMvc.perform(post("/api/users/admin/departments/{departmentId}/attributes/definitions", departmentId)
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attributeKey": "seatNo",
                                  "attributeName": "Seat Number",
                                  "dataType": "TEXT",
                                  "required": false,
                                  "displayOrder": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributeKey").value("seatNo"));

        mockMvc.perform(put("/api/users/admin/departments/{departmentId}/attributes", departmentId)
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attributes": {
                                    "costCenter": "CC-02"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.costCenter").value("CC-02"));

        mockMvc.perform(post("/api/users/admin/departments/{departmentId}/members", departmentId)
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].userId").value(userId));

        mockMvc.perform(put("/api/users/admin/departments/{departmentId}/members/{userId}/attributes", departmentId, userId)
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attributes": {
                                    "costCenter": "CC-02-MEMBER",
                                    "seatNo": "A-18"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberAttributes.costCenter").value("CC-02-MEMBER"))
                .andExpect(jsonPath("$.data.memberAttributes.seatNo").value("A-18"));

        mockMvc.perform(get("/api/users/admin/departments/users/{userId}/membership", userId)
                        .header("X-User-Id", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(departmentId))
                .andExpect(jsonPath("$.data.departmentCode").value("QA"));

        mockMvc.perform(post("/api/users/admin/departments/{departmentId}/members/attributes:batch", departmentId)
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operations": [
                                    {
                                      "userId": %d,
                                      "attributes": {
                                        "seatNo": "A-19"
                                      }
                                    }
                                  ]
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allSucceeded").value(true))
                .andExpect(jsonPath("$.data.results[0].attributes.seatNo").value("A-19"));

        mockMvc.perform(post("/api/users/admin/departments/transfer")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userIds": [%d],
                                  "fromDepartmentId": %d,
                                  "toDepartmentId": 20
                                }
                                """.formatted(userId, departmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].departmentCode").value("OPS"));

        mockMvc.perform(delete("/api/users/admin/departments/{departmentId}/attributes/definitions/{attributeKey}", departmentId, "costCenter")
                        .header("X-User-Id", ADMIN_USER_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/admin/departments/{departmentId}/attributes/definitions", departmentId)
                        .header("X-User-Id", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.attributeKey == 'costCenter')]").doesNotExist());
    }

    private void sendVerifyCode(String account, String contact, String scene) throws Exception {
        mockMvc.perform(post("/api/users/auth/verify-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "contact": "%s",
                                  "scene": "%s"
                                }
                                """.formatted(account, contact, scene)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scene").value(scene));
    }

    private void registerUser(String account, String password, String userName, String contact) throws Exception {
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "%s",
                                  "userName": "%s",
                                  "contact": "%s",
                                  "verifyCode": "123456",
                                  "agreeProtocol": true
                                }
                                """.formatted(account, password, userName, contact)))
                .andExpect(status().isOk());
    }

    private String loginAndExtractSessionKey(String account, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "%s"
                                }
                                """.formatted(account, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.sessionKey");
    }

    private Long createAdminUser(String account, String phone, Long departmentId, String userName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/admin")
                        .header("X-User-Id", ADMIN_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "member123",
                                  "userName": "%s",
                                  "email": "%s",
                                  "phone": "%s",
                                  "departmentId": %d,
                                  "roles": ["USER"]
                                }
                                """.formatted(account, userName, account, phone, departmentId)))
                .andExpect(status().isOk())
                .andReturn();
        return extractLong(result.getResponse().getContentAsString(), "$.data.userId");
    }

    private Long extractLong(String json, String path) {
        Number value = JsonPath.read(json, path);
        return value.longValue();
    }
}
