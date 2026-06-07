package com.example.platform.topbiz;

import com.example.platform.topbiz.domain.OrchestrationExecutionRecord;
import com.example.platform.topbiz.domain.OrchestrationStepRecord;
import com.example.platform.topbiz.repository.TopbizOrchestrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "topbiz.orchestration.repository-type=jdbc",
                "topbiz.orchestration.jdbc.url=jdbc:h2:mem:topbiz-orchestration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "topbiz.orchestration.jdbc.username=sa",
                "topbiz.orchestration.jdbc.password=",
                "topbiz.orchestration.jdbc.driver-class-name=org.h2.Driver",
                "spring.session.store-type=none",
                "spring.data.redis.repositories.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TopbizJdbcOrchestrationRepositoryTests {

    @Autowired
    private TopbizOrchestrationRepository topbizOrchestrationRepository;

    @Test
    void jdbcRepositoryShouldPersistAndReloadExecutionRecords() {
        Instant firstStartedAt = Instant.parse("2026-06-07T08:00:00Z");
        Instant secondStartedAt = Instant.parse("2026-06-07T09:00:00Z");
        OrchestrationExecutionRecord first = new OrchestrationExecutionRecord(
                "ORC-JDBC-001",
                "USER_PROVISIONING",
                "2001:new.staff",
                "SUCCESS",
                "trace-jdbc-001",
                firstStartedAt,
                firstStartedAt.plusSeconds(30),
                List.of(new OrchestrationStepRecord(
                        "CREATE_USER",
                        "Create User",
                        "SUCCESS",
                        "user created",
                        firstStartedAt,
                        firstStartedAt.plusSeconds(5),
                        Map.of("userId", 2001, "account", "new.staff")
                )),
                Map.of(
                        "auditLogged", true,
                        "summary", Map.of("userId", 2001, "status", "ENABLED")
                ),
                null,
                null
        );
        OrchestrationExecutionRecord second = new OrchestrationExecutionRecord(
                "ORC-JDBC-002",
                "MESSAGE_AUDIT",
                "message-88",
                "PARTIAL_SUCCESS",
                "trace-jdbc-002",
                secondStartedAt,
                secondStartedAt.plusSeconds(20),
                List.of(new OrchestrationStepRecord(
                        "SEND_MESSAGE",
                        "Send Message",
                        "SUCCESS",
                        "message sent",
                        secondStartedAt,
                        secondStartedAt.plusSeconds(3),
                        Map.of("messageId", "MSG-88", "channel", "EMAIL")
                )),
                Map.of("auditLogged", false),
                "AUDIT_WRITE_FAILED",
                "log backend unavailable"
        );

        topbizOrchestrationRepository.save(first);
        topbizOrchestrationRepository.save(second);

        OrchestrationExecutionRecord reloaded = topbizOrchestrationRepository.findById("ORC-JDBC-001")
                .orElseThrow();
        assertThat(reloaded.orchestrationType()).isEqualTo("USER_PROVISIONING");
        assertThat(reloaded.steps()).hasSize(1);
        assertThat(reloaded.steps().getFirst().detail()).containsEntry("userId", 2001);
        assertThat(reloaded.result()).containsEntry("auditLogged", true);

        List<OrchestrationExecutionRecord> byType = topbizOrchestrationRepository.findAll("MESSAGE_AUDIT", null, 10);
        assertThat(byType).hasSize(1);
        assertThat(byType.getFirst().orchestrationId()).isEqualTo("ORC-JDBC-002");

        List<OrchestrationExecutionRecord> ordered = topbizOrchestrationRepository.findAll(null, null, 10);
        assertThat(ordered).extracting(OrchestrationExecutionRecord::orchestrationId)
                .containsExactly("ORC-JDBC-002", "ORC-JDBC-001");
    }
}
