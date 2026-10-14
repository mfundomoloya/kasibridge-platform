package com.kasibridge.procurement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementAuditImmutabilityConfig {

    private static final String MUTATION_TRIGGER_NAME =
            "trg_procurement_audit_events_append_only";

    private static final String TRUNCATE_TRIGGER_NAME =
            "trg_procurement_audit_events_no_truncate";

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    public void installAuditImmutabilityProtection() {
        createMutationPreventionFunction();
        replaceMutationTrigger();
        replaceTruncateTrigger();

        log.info(
                "Installed append-only protection "
                        + "for procurement audit events"
        );
    }

    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    private void createMutationPreventionFunction() {
        jdbcTemplate.execute(
                """
                CREATE OR REPLACE FUNCTION
                    prevent_procurement_audit_mutation()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $function$
                BEGIN
                    IF current_setting(
                        'kasibridge.audit_maintenance',
                        true
                    ) = 'enabled' THEN
                        RETURN NULL;
                    END IF;

                    RAISE EXCEPTION
                        'Procurement audit events are append-only. % is not permitted.',
                        TG_OP
                        USING ERRCODE = '55000';
                END;
                $function$
                """
        );
    }

    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    private void replaceMutationTrigger() {
        dropTriggerIfPresent(
                MUTATION_TRIGGER_NAME
        );

        jdbcTemplate.execute(
                """
                CREATE TRIGGER
                    trg_procurement_audit_events_append_only
                BEFORE UPDATE OR DELETE
                ON procurement_audit_events
                FOR EACH ROW
                EXECUTE FUNCTION
                    prevent_procurement_audit_mutation()
                """
        );
    }

    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    private void replaceTruncateTrigger() {
        dropTriggerIfPresent(
                TRUNCATE_TRIGGER_NAME
        );

        jdbcTemplate.execute(
                """
                CREATE TRIGGER
                    trg_procurement_audit_events_no_truncate
                BEFORE TRUNCATE
                ON procurement_audit_events
                FOR EACH STATEMENT
                EXECUTE FUNCTION
                    prevent_procurement_audit_mutation()
                """
        );
    }

    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    private void dropTriggerIfPresent(
            String triggerName
    ) {
        Boolean triggerExists =
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM pg_trigger
                            WHERE tgname = ?
                              AND NOT tgisinternal
                        )
                        """,
                        Boolean.class,
                        triggerName
                );

        if (Boolean.TRUE.equals(triggerExists)) {
            String dropStatement =
                    "DROP TRIGGER "
                            + triggerName
                            + " ON "
                            + "procurement_audit_events";

            jdbcTemplate.execute(
                    dropStatement
            );
        }
    }
}