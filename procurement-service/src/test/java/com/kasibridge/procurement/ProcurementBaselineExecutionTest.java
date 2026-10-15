package com.kasibridge.procurement;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SuppressWarnings("resource")
class ProcurementBaselineExecutionTest {

    private static final String DATABASE_NAME =
            "procurement_baseline_test";

    private static final String DATABASE_USERNAME =
            "test";

    private static final String CONTAINER_BASELINE_PATH =
            "/tmp/V1__procurement_schema_baseline.sql";

    private static final Path BASELINE_FILE =
            Path.of(
                    "schema-draft",
                    "V1__procurement_schema_baseline.sql"
            );

    private static final Set<String> EXPECTED_TABLES =
            Set.of(
                    "bid_compliance_results",
                    "bid_evaluation_scores",
                    "bids",
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
                            DATABASE_NAME
                    )
                    .withUsername(
                            DATABASE_USERNAME
                    )
                    .withPassword("test");

    @Test
    void baselineShouldBuildCompleteSchemaOnEmptyDatabase()
            throws Exception {
        assertTrue(
                Files.exists(BASELINE_FILE),
                () ->
                        "Baseline file does not exist: "
                                + BASELINE_FILE
                                .toAbsolutePath()
        );

        POSTGRES.copyFileToContainer(
                MountableFile.forHostPath(
                        BASELINE_FILE
                ),
                CONTAINER_BASELINE_PATH
        );

        org.testcontainers.containers.Container.ExecResult
                migrationResult =
                executePsqlFile(
                        CONTAINER_BASELINE_PATH
                );

        assertEquals(
                0,
                migrationResult.getExitCode(),
                () ->
                        "Baseline execution failed:\n"
                                + migrationResult
                                .getStderr()
        );

        assertExpectedTablesExist();
        assertIdentityColumnsExist();
        assertRequiredConstraintsExist();
        assertRequiredIndexesExist();
        assertAuditProtectionExists();
        assertAuditMutationIsBlocked();
    }

    private org.testcontainers.containers.Container.ExecResult
    executePsqlFile(
            String containerFile
    ) throws Exception {
        return POSTGRES.execInContainer(
                "psql",
                "--set=ON_ERROR_STOP=1",
                "--username="
                        + DATABASE_USERNAME,
                "--dbname="
                        + DATABASE_NAME,
                "--file="
                        + containerFile
        );
    }

    private void assertExpectedTablesExist()
            throws Exception {
        org.testcontainers.containers.Container.ExecResult
                result =
                executeQuery(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_type = 'BASE TABLE'
                        ORDER BY table_name;
                        """
                );

        assertSuccessfulQuery(
                result,
                "table query"
        );

        Set<String> actualTables =
                result.getStdout()
                        .lines()
                        .map(String::trim)
                        .filter(value ->
                                !value.isBlank()
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                EXPECTED_TABLES,
                actualTables
        );
    }

    private void assertIdentityColumnsExist()
            throws Exception {
        org.testcontainers.containers.Container.ExecResult
                result =
                executeQuery(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND column_name = 'id'
                          AND is_identity = 'YES';
                        """
                );

        assertSuccessfulQuery(
                result,
                "identity-column query"
        );

        assertEquals(
                "10",
                result.getStdout().trim()
        );
    }

    private void assertRequiredConstraintsExist()
            throws Exception {
        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'uk_notification_template_ticket_recipient';
                """,
                "notification uniqueness constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'uk_ticket_ai_assessment_ticket_id';
                """,
                "AI assessment ticket uniqueness constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'uk_eval_bid_evaluator';
                """,
                "evaluation uniqueness constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'procurement_audit_events_event_type_check';
                """,
                "audit event type constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'procurement_audit_events_result_check';
                """,
                "audit result constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'bids_status_check';
                """,
                "bid status constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'notification_outbox_template_type_check';
                """,
                "notification template constraint"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname =
                    'support_ticket_ai_assessments_assessment_status_check';
                """,
                "AI assessment status constraint"
        );
    }

    private void assertRequiredIndexesExist()
            throws Exception {
        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname =
                    'uq_active_bid_per_trader_tender';
                """,
                "active bid partial unique index"
        );

        org.testcontainers.containers.Container.ExecResult
                definitionResult =
                executeQuery(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND indexname =
                            'uq_active_bid_per_trader_tender';
                        """
                );

        assertSuccessfulQuery(
                definitionResult,
                "active bid index definition query"
        );

        String indexDefinition =
                definitionResult
                        .getStdout()
                        .trim();

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
                        "status"
                )
        );

        assertTrue(
                indexDefinition.contains(
                        "WITHDRAWN"
                )
        );
    }

    private void assertAuditProtectionExists()
            throws Exception {
        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_proc
                WHERE proname =
                    'prevent_procurement_audit_mutation';
                """,
                "audit mutation-prevention function"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_trigger
                WHERE tgname =
                    'trg_procurement_audit_events_append_only'
                  AND NOT tgisinternal;
                """,
                "audit update and delete trigger"
        );

        assertDatabaseObjectExists(
                """
                SELECT COUNT(*)
                FROM pg_trigger
                WHERE tgname =
                    'trg_procurement_audit_events_no_truncate'
                  AND NOT tgisinternal;
                """,
                "audit truncate trigger"
        );
    }

    private void assertAuditMutationIsBlocked()
            throws Exception {
        org.testcontainers.containers.Container.ExecResult
                insertResult =
                executeQuery(
                        """
                        INSERT INTO
                            public.procurement_audit_events (
                                event_type,
                                actor_user_id,
                                result,
                                message,
                                event_hash,
                                created_at
                            )
                        VALUES (
                            'SUPPORT_TICKET_CREATED',
                            73,
                            'SUCCESS',
                            'Baseline immutability test',
                            'baseline-test-hash',
                            CURRENT_TIMESTAMP
                        );

                        SELECT COUNT(*)
                        FROM public.procurement_audit_events;
                        """
                );

        assertSuccessfulQuery(
                insertResult,
                "audit insert query"
        );

        assertTrue(
                insertResult
                        .getStdout()
                        .trim()
                        .endsWith("1")
        );

        org.testcontainers.containers.Container.ExecResult
                updateResult =
                executeQueryAllowingFailure(
                        """
                        UPDATE public.procurement_audit_events
                        SET message = 'Tampered message';
                        """
                );

        assertTrue(
                updateResult.getExitCode() != 0,
                "Audit update should have failed"
        );

        assertTrue(
                updateResult
                        .getStderr()
                        .contains(
                                "Procurement audit events "
                                        + "are append-only"
                        )
        );
    }

    private void assertDatabaseObjectExists(
            String query,
            String description
    ) throws Exception {
        org.testcontainers.containers.Container.ExecResult
                result =
                executeQuery(query);

        assertSuccessfulQuery(
                result,
                description
        );

        assertEquals(
                "1",
                result.getStdout().trim(),
                () ->
                        "Missing database object: "
                                + description
        );
    }

    private org.testcontainers.containers.Container.ExecResult
    executeQuery(
            String query
    ) throws Exception {
        return POSTGRES.execInContainer(
                "psql",
                "--tuples-only",
                "--no-align",
                "--set=ON_ERROR_STOP=1",
                "--username="
                        + DATABASE_USERNAME,
                "--dbname="
                        + DATABASE_NAME,
                "--command="
                        + query
        );
    }

    private org.testcontainers.containers.Container.ExecResult
    executeQueryAllowingFailure(
            String query
    ) throws Exception {
        return POSTGRES.execInContainer(
                "psql",
                "--tuples-only",
                "--no-align",
                "--username="
                        + DATABASE_USERNAME,
                "--dbname="
                        + DATABASE_NAME,
                "--command="
                        + query
        );
    }

    private void assertSuccessfulQuery(
            org.testcontainers.containers.Container.ExecResult
                    result,
            String description
    ) {
        assertEquals(
                0,
                result.getExitCode(),
                () ->
                        "Failed "
                                + description
                                + ":\n"
                                + result.getStderr()
        );
    }
}