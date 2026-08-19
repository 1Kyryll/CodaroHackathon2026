package com.example.hackathoncodaro2026;

import com.example.hackathoncodaro2026.service.AuditLogService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogServiceTests {

    private final AuditLogService auditLogService = new AuditLogService();

    @Test
    void sanitizesCarriageReturnAndLineFeed() {
        String cleaned = auditLogService.sanitize("ok\r\ninjected\tvalue");
        assertThat(cleaned).doesNotContain("\r");
        assertThat(cleaned).doesNotContain("\n");
        assertThat(cleaned).doesNotContain("\t");
        assertThat(cleaned).isEqualTo("ok injected value");
    }

    @Test
    void truncatesLongValues() {
        String cleaned = auditLogService.sanitize("x".repeat(400));
        assertThat(cleaned).hasSize(180);
    }

    @Test
    void masksEmailAndDoesNotEmitRawAddress() {
        String masked = auditLogService.maskEmail("player@example.com");
        assertThat(masked).isNotEqualTo("player@example.com");
        assertThat(masked).doesNotContain("player@");
        assertThat(masked).contains("@");
        assertThat(masked).startsWith("p***@");
    }

    @Test
    void masksPhoneAndKeepsOnlyLastTwoDigits() {
        String masked = auditLogService.maskPhone("+48 555 010 050");
        assertThat(masked).isEqualTo("***50");
        assertThat(masked).doesNotContain("555");
        assertThat(masked).doesNotContain("010");
    }

    @Test
    void recordStripsInjectionFromLoggedMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuditLogService.LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            auditLogService.record(
                    "REGISTER",
                    "user\nATTACK",
                    "USER",
                    "USER",
                    12L,
                    "SUCCESS",
                    Map.of("note", "line1\r\nline2")
            );
            assertThat(appender.list).isNotEmpty();
            String message = appender.list.get(appender.list.size() - 1).getFormattedMessage();
            assertThat(message).doesNotContain("\r");
            assertThat(message).doesNotContain("\n");
            assertThat(message).contains("event=REGISTER");
            assertThat(message).contains("actor=\"user ATTACK\"");
            assertThat(message).contains("note=\"line1 line2\"");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void recordDoesNotIncludeRawEmailWhenMaskedDetailIsUsed() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuditLogService.LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String raw = "player@example.com";
            auditLogService.record(
                    "REGISTER",
                    "playerone",
                    "USER",
                    "USER",
                    1L,
                    "REJECTED",
                    Map.of("email", auditLogService.maskEmail(raw), "field", "email")
            );
            String message = appender.list.get(appender.list.size() - 1).getFormattedMessage();
            assertThat(message).doesNotContain(raw);
            assertThat(message).contains("field=email");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
