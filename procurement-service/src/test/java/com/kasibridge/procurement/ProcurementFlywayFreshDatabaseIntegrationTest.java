package com.kasibridge.procurement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

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
    void flywayShouldBuildAndValidateFreshProcurementDatabase() {
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(
                        dataSource
                );

        assertNotNull(
                dataSource
        );

        assertExpectedTablesExist(
                jdbcTemplate
        );

        assertExpectedMigrationsApplied();

        flyway.validate();

        assertActiveBidIndexExists(
                jdbcTemplate
        );

        assertAuditProtectionExists(
                jdbcTemplate
        );

        assertNotificationClaimColumnsExist(
                jdbcTemplate
        );

        assertNotificationStatusConstraintExists(
                jdbcTemplate
        );

        assertNotificationClaimIndexesExist(
                jdbcTemplate
        );
    }

    private void assertExpectedTablesExist(
            JdbcTemplate jdbcTemplate
    ) {
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
    }

    private void assertExpectedMigrationsApplied() {
        MigrationInfo[] appliedMigrations =
                flyway.info().applied();

        assertEquals(
                3,
                appliedMigrations.length
        );

        MigrationInfo baselineMigration =
                findAppliedMigration(
                        appliedMigrations,
                        "1"
                );

        assertEquals(
                "procurement schema baseline",
                baselineMigration.getDescription()
        );

        assertTrue(
                baselineMigration
                        .getState()
                        .isApplied()
        );

        assertNotNull(
                baselineMigration.getInstalledOn()
        );

        MigrationInfo claimMigration =
                findAppliedMigration(
                        appliedMigrations,
                        "2"
                );

        assertEquals(
                "add notification dispatch claiming",
                claimMigration.getDescription()
        );

        assertTrue(
                claimMigration
                        .getState()
                        .isApplied()
        );

        assertNotNull(
                claimMigration.getInstalledOn()
        );
    }

    private MigrationInfo findAppliedMigration(
            MigrationInfo[] migrations,
            String version
    ) {
        return Arrays.stream(
                        migrations
                )
                .filter(info ->
                        info.getVersion() != null
                                && version.equals(
                                info.getVersion()
                                        .getVersion()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Expected Flyway migration "
                                        + "version "
                                        + version
                                        + " to be applied"
                        )
                );
    }

    private void assertActiveBidIndexExists(
            JdbcTemplate jdbcTemplate
    ) {
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

        String indexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND indexname =
                            'uq_active_bid_per_trader_tender'
                        """,
                        String.class
                );

        assertNotNull(
                indexDefinition
        );

        assertTrue(
                indexDefinition.contains(
                        "tender_id"
                )
        );

        assertTrue(
                indexDefinition.contains(
                        "trader_profile_id"
                )
        );

        assertTrue(
                indexDefinition.contains(
                        "WITHDRAWN"
                )
        );
    }

    private void assertAuditProtectionExists(
            JdbcTemplate jdbcTemplate
    ) {
        Integer auditFunctionCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM pg_proc
                        WHERE proname =
                            'prevent_procurement_audit_mutation'
                        """,
                        Integer.class
                );

        assertEquals(
                1,
                auditFunctionCount
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
    }

    private void assertNotificationClaimColumnsExist(
            JdbcTemplate jdbcTemplate
    ) {
        Integer claimColumnCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name =
                            'notification_outbox'
                          AND column_name IN (
                            'claimed_at',
                            'claimed_by',
                            'claim_expires_at',
                            'next_attempt_at',
                            'version'
                          )
                        """,
                        Integer.class
                );

        assertEquals(
                5,
                claimColumnCount
        );

        String claimedByLength =
                jdbcTemplate.queryForObject(
                        """
                        SELECT character_maximum_length::text
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name =
                            'notification_outbox'
                          AND column_name =
                            'claimed_by'
                        """,
                        String.class
                );

        assertEquals(
                "100",
                claimedByLength
        );

        String versionNullable =
                jdbcTemplate.queryForObject(
                        """
                        SELECT is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name =
                            'notification_outbox'
                          AND column_name =
                            'version'
                        """,
                        String.class
                );

        assertEquals(
                "NO",
                versionNullable
        );

        String versionDefault =
                jdbcTemplate.queryForObject(
                        """
                        SELECT column_default
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name =
                            'notification_outbox'
                          AND column_name =
                            'version'
                        """,
                        String.class
                );

        assertNotNull(
                versionDefault
        );

        assertTrue(
                versionDefault.contains("0")
        );
    }

    private void assertNotificationStatusConstraintExists(
            JdbcTemplate jdbcTemplate
    ) {
        Integer statusConstraintCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM pg_constraint
                        WHERE conname =
                            'notification_outbox_status_check'
                        """,
                        Integer.class
                );

        assertEquals(
                1,
                statusConstraintCount
        );

        String constraintDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT pg_get_constraintdef(oid)
                        FROM pg_constraint
                        WHERE conname =
                            'notification_outbox_status_check'
                        """,
                        String.class
                );

        assertNotNull(
                constraintDefinition
        );

        assertTrue(
                constraintDefinition.contains(
                        "PENDING"
                )
        );

        assertTrue(
                constraintDefinition.contains(
                        "PROCESSING"
                )
        );

        assertTrue(
                constraintDefinition.contains(
                        "SENT"
                )
        );

        assertTrue(
                constraintDefinition.contains(
                        "FAILED"
                )
        );

        assertTrue(
                constraintDefinition.contains(
                        "CANCELLED"
                )
        );
    }

    private void assertNotificationClaimIndexesExist(
            JdbcTemplate jdbcTemplate
    ) {
        Integer claimIndexCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND indexname IN (
                            'idx_notification_dispatch_queue',
                            'idx_notification_claim_expiry'
                          )
                        """,
                        Integer.class
                );

        assertEquals(
                2,
                claimIndexCount
        );

        String dispatchIndexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND indexname =
                            'idx_notification_dispatch_queue'
                        """,
                        String.class
                );

        assertNotNull(
                dispatchIndexDefinition
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "channel"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "status"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "next_attempt_at"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "created_at"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "id"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "PENDING"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "PROCESSING"
                )
        );

        assertTrue(
                dispatchIndexDefinition.contains(
                        "FAILED"
                )
        );

        String claimExpiryIndexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND indexname =
                            'idx_notification_claim_expiry'
                        """,
                        String.class
                );

        assertNotNull(
                claimExpiryIndexDefinition
        );

        assertTrue(
                claimExpiryIndexDefinition.contains(
                        "claim_expires_at"
                )
        );

        assertTrue(
                claimExpiryIndexDefinition.contains(
                        "id"
                )
        );

        assertTrue(
                claimExpiryIndexDefinition.contains(
                        "PROCESSING"
                )
        );
    }
}