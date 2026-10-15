package com.kasibridge.procurement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.validate-on-migrate=true",
                "spring.flyway.clean-disabled=true",
                "kasibridge.notifications.dispatch.enabled=false",
                "kasibridge.notifications.mock-whatsapp.enabled=true",
                "kasibridge.notifications.whatsapp.enabled=false",
                "kasibridge.ticket-ai.auto-assessment.enabled=false",
                "kasibridge.internal.system.username=test-system",
                "kasibridge.internal.system.password=test-password"
        }
)
@Testcontainers
@SuppressWarnings({
        "resource",
        "SpringBootApplicationProperties"
})
class ProcurementFlywayFreshDatabaseIntegrationTest {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    private static final Set<String> EXPECTED_TABLES =
            Set.of(
                    "bid_compliance_results",
                    "bid_evaluation_scores",
                    "bids",
                    "flyway_schema_history",
                    "notification_outbox",
                    "procurement_anomalies",
                    "procurement_audit_events",
                    "support_ticket_ai_assessments",
                    "support_tickets",
                    "tender_committee_assignments",
                    "tenders"
            );

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "procurement_flyway_fresh_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }

    @Test
    void flywayShouldBuildAndValidateFreshProcurementDatabase(){
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(dataSource);

        Set<String> actualTables =
                jdbcTemplate.queryForList(
                                """
                                SELECT table_name
                                FROM information_schema.tables
                                WHERE table_schema = 'public'
                                  AND table_type = 'BASE TABLE'
                                """,
                                String.class
                        )
                        .stream()
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                EXPECTED_TABLES,
                actualTables
        );

        MigrationInfo[] appliedMigrations =
                flyway.info().applied();

        assertEquals(
                1,
                appliedMigrations.length
        );

        MigrationInfo baselineMigration =
                Arrays.stream(
                                appliedMigrations
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "1",
                baselineMigration
                        .getVersion()
                        .getVersion()
        );

        assertEquals(
                "procurement schema baseline",
                baselineMigration
                        .getDescription()
        );

        assertTrue(
                baselineMigration
                        .getState()
                        .isApplied()
        );

        flyway.validate();

        Integer activeBidIndexCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND indexname =
                            'uq_active_bid_per_trader_tender'
                        """,
                        Integer.class
                );

        assertEquals(
                1,
                activeBidIndexCount
        );

        Integer auditTriggerCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM pg_trigger
                        WHERE tgname IN (
                            'trg_procurement_audit_events_append_only',
                            'trg_procurement_audit_events_no_truncate'
                        )
                          AND NOT tgisinternal
                        """,
                        Integer.class
                );

        assertEquals(
                2,
                auditTriggerCount
        );

        assertNotNull(
                dataSource
        );
    }
}