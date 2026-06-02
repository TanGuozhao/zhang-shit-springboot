package com.example.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceApplicationTests {

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
    void getDepartmentShouldReturnDepartmentInfo() throws Exception {
        mockMvc.perform(get("/api/users/departments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentCode").value("PLATFORM"));
    }

    @Test
    void createUserShouldPersistInMemory() throws Exception {
        mockMvc.perform(post("/api/users/admin")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["OPS_ADMIN"],
                                  "permissions": ["message:send", "log:query", "user:read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("OPS_ADMIN"));

        mockMvc.perform(post("/api/users/admin/1002/authorization/permissions:refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions[2]").value("user:read"));
    }
}
