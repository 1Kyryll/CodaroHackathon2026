package com.example.hackathoncodaro2026;

import com.example.hackathoncodaro2026.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ConfigAvatarAndLoggingGuardTests {

    @Autowired
    private Environment environment;

    @Autowired
    private AuditLogService auditLogService;

    @Test
    void testProfileDisablesBrowserAndHasNoApplicationProperties() {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("app.browser.auto-open", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isFalse();
        assertThat(getClass().getResource("/application.properties")).isNull();
        assertThat(getClass().getClassLoader().getResource("application.properties")).isNull();
    }

    @Test
    void testsDoNotWriteProductionLogFiles() {
        assertThat(environment.getProperty("app.logging.dir")).isEqualTo("./build/test-logs");
        assertThat(getClass().getClassLoader().getResource("logback-test.xml")).isNotNull();
        assertThat(Files.exists(Path.of("build", "test-logs", "courtly.log"))).isFalse();
        assertThat(Files.exists(Path.of("build", "test-logs", "courtly-audit.log"))).isFalse();
    }

    @Test
    void auditRecordOmitsBlankSecretsAndKeepsStructuredEvent() {
        auditLogService.record(
                "PASSWORD_CHANGE",
                "cfg_actor",
                "USER",
                "USER",
                9L,
                "SUCCESS",
                Map.of("changed", true, "password", "", "currentPassword", "")
        );
        assertThat(auditLogService.sanitize("ok")).isEqualTo("ok");
        assertThat(auditLogService.maskEmail("admin@sportsfacility.local")).doesNotContain("admin@sportsfacility.local");
        assertThat(auditLogService.maskPhone("+48 22 621 00 01")).startsWith("***");
    }
}
