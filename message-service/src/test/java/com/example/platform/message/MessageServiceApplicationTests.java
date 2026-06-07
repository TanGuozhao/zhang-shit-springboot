package com.example.platform.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessageServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoadArchitectureOverviewAndTemplateCatalog() throws Exception {
        mockMvc.perform(get("/internal/architecture/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("message-service"))
                .andExpect(jsonPath("$.data.boundedContext").value("message-channel"))
                .andExpect(jsonPath("$.data.publicBasePath").value("/api/messages"));

        mockMvc.perform(get("/api/messages/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].templateCode").value("WELCOME"))
                .andExpect(jsonPath("$.data[0].description").exists());

        mockMvc.perform(get("/api/messages/templates/WELCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjectTemplate").value("Welcome aboard"))
                .andExpect(jsonPath("$.data.variables[0].variableCode").value("userName"));

        mockMvc.perform(get("/api/messages/templates/WELCOME/variables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].variableCode").value("userName"));
    }

    @Test
    void shouldPreviewValidateResolveAndSaveDraft() throws Exception {
        mockMvc.perform(post("/api/messages/variables/fill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "ALERT_NOTICE",
                                  "variables": {
                                    "alertCode": "ERR-700"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.renderedContent").value(org.hamcrest.Matchers.containsString("ERR-700")))
                .andExpect(jsonPath("$.data.resolvedVariables.currentTime").exists());

        mockMvc.perform(post("/api/messages/variables/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "BILLING_REMINDER",
                                  "variables": {
                                    "userName": "Zhang San"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errors[0]").value(org.hamcrest.Matchers.containsString("invoiceNo")));

        mockMvc.perform(post("/api/messages/templates/WELCOME/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "variables": {
                                    "userName": "Preview User"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.renderedSubject").value("Welcome aboard"))
                .andExpect(jsonPath("$.data.renderedContent").value(org.hamcrest.Matchers.containsString("Preview User")));

        mockMvc.perform(post("/api/messages/receivers/resolve")
                        .param("channel", "SMS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receivers": ["13800000000"],
                                  "receiverGroups": ["SMS_TESTERS"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receivers.length()").value(2))
                .andExpect(jsonPath("$.data.groups[0].groupCode").value("SMS_TESTERS"));

        mockMvc.perform(post("/api/messages/schedule/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dispatchType": "SCHEDULED",
                                  "scheduledAt": "%s"
                                }
                                """.formatted(Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.dispatchType").value("SCHEDULED"));

        mockMvc.perform(post("/api/messages/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "BILLING_REMINDER",
                                  "channel": "EMAIL",
                                  "receivers": ["finance@example.com"],
                                  "variables": {
                                    "userName": "Li Si",
                                    "invoiceNo": "INV-20260607",
                                    "dueDate": "2026-06-30"
                                  },
                                  "attachmentIds": ["ATT-DRAFT-001"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("INV-20260607")))
                .andExpect(jsonPath("$.data.attachmentIds[0]").value("ATT-DRAFT-001"));
    }

    @Test
    void shouldSendImmediateMessageQueryStatisticsAndManageInbox() throws Exception {
        MvcResult sendResult = mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "WELCOME",
                                  "channel": "EMAIL",
                                  "receivers": ["new.user@example.com"],
                                  "variables": {
                                    "userName": "New User"
                                  },
                                  "attachmentIds": ["ATT-EMAIL-001"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.subject").value("Welcome aboard"))
                .andReturn();

        JsonNode sendBody = readBody(sendResult);
        String messageId = sendBody.path("data").path("messageId").asText();
        assertThat(messageId).startsWith("MSG-");

        mockMvc.perform(get("/api/messages/" + messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value("WELCOME"))
                .andExpect(jsonPath("$.data.attachmentIds[0]").value("ATT-EMAIL-001"));

        mockMvc.perform(get("/api/messages/" + messageId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.dispatchType").value("IMMEDIATE"))
                .andExpect(jsonPath("$.data.sentAt").exists());

        mockMvc.perform(get("/api/messages/records").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/messages/search").param("keyword", "New User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].messageId").exists());

        MvcResult inboxListResult = mockMvc.perform(get("/api/messages/inbox").param("receiver", "new.user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn();

        String inboxId = findInboxIdByMessageId(readBody(inboxListResult), messageId);
        assertThat(inboxId).isNotBlank();

        mockMvc.perform(get("/api/messages/inbox/" + inboxId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageId").value(messageId))
                .andExpect(jsonPath("$.data.readStatus").value("UNREAD"));

        mockMvc.perform(put("/api/messages/inbox/" + inboxId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readStatus").value("READ"))
                .andExpect(jsonPath("$.data.readAt").exists());

        mockMvc.perform(get("/api/messages/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMessages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.successfulMessages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.channelBreakdown.EMAIL").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldManageAdminConfigurationAndDispatchTasks() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String variableCode = "customVar" + suffix;
        String templateCode = "CUSTOM_" + suffix;
        String channelCode = "EMAIL_" + suffix;
        String taskCode = "ADMIN_TASK_" + suffix;

        mockMvc.perform(post("/api/messages/admin/variables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "variableCode": "%s",
                                  "variableName": "Custom Variable",
                                  "description": "Custom variable for admin flow.",
                                  "dataType": "NUMBER",
                                  "defaultValue": "7",
                                  "required": true,
                                  "enabled": true,
                                  "autoFill": false
                                }
                                """.formatted(variableCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variableCode").value(variableCode))
                .andExpect(jsonPath("$.data.dataType").value("NUMBER"));

        mockMvc.perform(put("/api/messages/admin/variables/" + variableCode + "/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataType": "TEXT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataType").value("TEXT"));

        mockMvc.perform(put("/api/messages/admin/variables/" + variableCode + "/required")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "required": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.required").value(false));

        mockMvc.perform(post("/api/messages/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "%s",
                                  "templateName": "Custom Admin Template",
                                  "channel": "EMAIL",
                                  "subjectTemplate": "Admin subject",
                                  "contentTemplate": "Value is {{%s}}.",
                                  "description": "Template managed by admin endpoints.",
                                  "variableCodes": ["%s"],
                                  "enabled": true
                                }
                                """.formatted(templateCode, variableCode, variableCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value(templateCode))
                .andExpect(jsonPath("$.data.variables[0].variableCode").value(variableCode));

        mockMvc.perform(post("/api/messages/admin/carrier/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierName": "Mail Provider",
                                  "channelType": "EMAIL",
                                  "accountCode": "acct-%s",
                                  "apiKey": "secret",
                                  "endpoint": "https://mail.example.com",
                                  "signature": "mail-signature",
                                  "enabled": true
                                }
                                """.formatted(suffix.toLowerCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountCode").value("acct-" + suffix.toLowerCase()));

        mockMvc.perform(post("/api/messages/admin/channels/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channelCode": "%s",
                                  "channelType": "EMAIL",
                                  "carrierName": "Mail Provider",
                                  "accountCode": "acct-%s",
                                  "sender": "ops@example.com",
                                  "enabled": true,
                                  "healthy": true,
                                  "description": "Custom email channel"
                                }
                                """.formatted(channelCode, suffix.toLowerCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelCode").value(channelCode))
                .andExpect(jsonPath("$.data.sender").value("ops@example.com"));

        mockMvc.perform(put("/api/messages/admin/channels/" + channelCode + "/sender")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sender": "updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sender").value("updated@example.com"));

        mockMvc.perform(post("/api/messages/admin/schedule/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyCode": "POLICY_%s",
                                  "cronExpression": "0 */10 * * * *",
                                  "policyType": "CRON",
                                  "enabled": true,
                                  "description": "Admin-created cron policy"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyCode").value("POLICY_" + suffix))
                .andExpect(jsonPath("$.data.policyType").value("CRON"));

        MvcResult taskResult = mockMvc.perform(post("/api/messages/admin/dispatch/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskCode": "%s",
                                  "messageId": "MSG-SEED-1001",
                                  "channelCode": "EMAIL",
                                  "channelAccountCode": "email-main",
                                  "plannedAt": "%s",
                                  "sortOrder": 9
                                }
                                """.formatted(taskCode, Instant.now().plusSeconds(120))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskCode").value(taskCode))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String taskId = readBody(taskResult).path("data").path("taskId").asText();
        assertThat(taskId).startsWith("DST-");

        mockMvc.perform(get("/api/messages/admin/dispatch/tasks").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(put("/api/messages/admin/dispatch/tasks/" + taskId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(put("/api/messages/admin/templates/" + templateCode + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "%s",
                                  "channel": "EMAIL",
                                  "receivers": ["blocked@example.com"],
                                  "variables": {
                                    "%s": "blocked"
                                  }
                                }
                                """.formatted(templateCode, variableCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEMPLATE_DISABLED"));
    }

    @Test
    void shouldRunScheduledDispatchAndExposeRuntimeState() throws Exception {
        MvcResult sendResult = mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "WELCOME",
                                  "channel": "EMAIL",
                                  "receivers": ["ops@example.com"],
                                  "receiverGroups": ["OPS_TEAM"],
                                  "variables": {
                                    "userName": "Ops Team"
                                  },
                                  "dispatchType": "SCHEDULED",
                                  "scheduledAt": "%s",
                                  "attachmentIds": ["ATT-SCHEDULED-001"]
                                }
                                """.formatted(Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.dispatchType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.batchCode").exists())
                .andReturn();

        String messageId = readBody(sendResult).path("data").path("messageId").asText();
        assertThat(messageId).startsWith("MSG-");

        mockMvc.perform(get("/api/messages/" + messageId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        MvcResult listResult = mockMvc.perform(get("/api/messages/admin/dispatch/tasks").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tasks = readBody(listResult).path("data").path("records");
        int updatedCount = 0;
        for (JsonNode task : tasks) {
            if (messageId.equals(task.path("messageId").asText())) {
                updatedCount++;
                mockMvc.perform(put("/api/messages/admin/dispatch/tasks/" + task.path("taskId").asText() + "/time")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "plannedAt": "%s"
                                        }
                                        """.formatted(Instant.now().minusSeconds(30))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("PENDING"));
            }
        }
        assertThat(updatedCount).isGreaterThanOrEqualTo(2);

        mockMvc.perform(post("/api/messages/internal/tasks/dispatch/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/messages/" + messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.sentAt").exists());

        mockMvc.perform(get("/api/messages/internal/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastDispatchRunAt").exists())
                .andExpect(jsonPath("$.data.inboxMessages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void shouldRetryTransientFailuresUntilSuccess() throws Exception {
        MvcResult sendResult = mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "WELCOME",
                                  "channel": "EMAIL",
                                  "receivers": ["fail-once@example.com"],
                                  "variables": {
                                    "userName": "Retry User"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andReturn();

        String messageId = readBody(sendResult).path("data").path("messageId").asText();
        assertThat(messageId).startsWith("MSG-");

        mockMvc.perform(get("/api/messages/" + messageId + "/error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.errorCode").value("CHANNEL_DELIVERY_FAILED"))
                .andExpect(jsonPath("$.data.retryable").value(true));

        mockMvc.perform(post("/api/messages/admin/retries/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processedCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.successCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/messages/" + messageId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.sentAt").exists());

        mockMvc.perform(get("/api/messages/internal/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingRetryRecords").value(0))
                .andExpect(jsonPath("$.data.lastRetryRunAt").exists());
    }

    @Test
    void cancellingAllDispatchTasksShouldCancelScheduledMessage() throws Exception {
        MvcResult sendResult = mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "WELCOME",
                                  "channel": "EMAIL",
                                  "receivers": ["cancel-me@example.com"],
                                  "dispatchType": "SCHEDULED",
                                  "scheduledAt": "%s",
                                  "variables": {
                                    "userName": "Cancel User"
                                  }
                                }
                                """.formatted(Instant.now().plusSeconds(900))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn();

        String messageId = readBody(sendResult).path("data").path("messageId").asText();

        MvcResult listResult = mockMvc.perform(get("/api/messages/admin/dispatch/tasks").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andReturn();
        String taskId = "";
        Iterator<JsonNode> iterator = readBody(listResult).path("data").path("records").elements();
        while (iterator.hasNext()) {
            JsonNode task = iterator.next();
            if (messageId.equals(task.path("messageId").asText())) {
                taskId = task.path("taskId").asText();
                break;
            }
        }
        assertThat(taskId).isNotBlank();

        mockMvc.perform(put("/api/messages/admin/dispatch/tasks/" + taskId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/messages/" + messageId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String findInboxIdByMessageId(JsonNode inboxListBody, String messageId) {
        Iterator<JsonNode> iterator = inboxListBody.path("data").path("records").elements();
        while (iterator.hasNext()) {
            JsonNode record = iterator.next();
            if (messageId.equals(record.path("messageId").asText())) {
                return record.path("inboxId").asText();
            }
        }
        return "";
    }
}
