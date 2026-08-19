package com.example.hackathoncodaro2026;

import com.example.hackathoncodaro2026.config.SchemaEnumConstraintRepair;
import com.example.hackathoncodaro2026.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class SchemaEnumConstraintRepairTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaEnumConstraintRepair schemaEnumConstraintRepair;

    @Autowired
    private UserRepository userRepository;

    @Test
    void dropsLegacyRoleCheckAndKeepsManager() {
        String table = tableName("APP_USERS");
        jdbcTemplate.execute(
                "ALTER TABLE \"" + table + "\" ADD CONSTRAINT app_users_role_legacy CHECK (\"ROLE\" IN ('ADMIN', 'USER', 'MANAGER'))"
        );
        assertThat(checkExists("APP_USERS", "APP_USERS_ROLE_LEGACY") || checkExists("APP_USERS", "app_users_role_legacy")).isTrue();
        schemaEnumConstraintRepair.run(new DefaultApplicationArguments());
        assertThat(checkExists("APP_USERS", "APP_USERS_ROLE_LEGACY") || checkExists("APP_USERS", "app_users_role_legacy")).isFalse();
        assertThat(userRepository.existsByUsernameIgnoreCase("manager")).isTrue();
        assertThatCode(() -> schemaEnumConstraintRepair.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void dropsLegacyPaymentMethodCheck() {
        String table = tableName("RESERVATIONS");
        jdbcTemplate.execute(
                "ALTER TABLE \"" + table + "\" ADD CONSTRAINT reservations_payment_method_legacy CHECK (\"PAYMENT_METHOD\" IN ('CASH', 'CARD_ON_SITE', 'ONLINE_TRANSFER'))"
        );
        assertThat(checkExists("RESERVATIONS", "RESERVATIONS_PAYMENT_METHOD_LEGACY")
                || checkExists("RESERVATIONS", "reservations_payment_method_legacy")).isTrue();
        schemaEnumConstraintRepair.run(new DefaultApplicationArguments());
        assertThat(checkExists("RESERVATIONS", "RESERVATIONS_PAYMENT_METHOD_LEGACY")
                || checkExists("RESERVATIONS", "reservations_payment_method_legacy")).isFalse();
        assertThatCode(() -> schemaEnumConstraintRepair.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    private boolean checkExists(String table, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                        WHERE CONSTRAINT_TYPE = 'CHECK'
                          AND UPPER(TABLE_NAME) = ?
                          AND UPPER(CONSTRAINT_NAME) = ?
                        """,
                Integer.class,
                table.toUpperCase(),
                constraintName.toUpperCase()
        );
        return count != null && count > 0;
    }

    private String tableName(String table) {
        return jdbcTemplate.query(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?",
                rs -> rs.next() ? rs.getString("TABLE_NAME") : table,
                table
        );
    }
}
