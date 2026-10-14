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

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void installOnApplicationReady() {
        installAuditImmutabilityProtection();
    }

    @Transactional
    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    public void installAuditImmutabilityProtection() {
        jdbcTemplate.execute(
                """
                CREATE OR REPLACE FUNCTION
                    prevent_procurement_audit_mutation()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $function$
                BEGIN
                    RAISE EXCEPTION
                        'Procurement audit events are append-only. % is not permitted.',
                        TG_OP
                        USING ERRCODE = '55000';
                END;
                $function$
                """
        );

        removeAuditImmutabilityProtection();

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

        log.info(
                "Installed append-only protection for "
                        + "procurement audit events"
        );
    }

    @Transactional
    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    public void removeAuditImmutabilityProtection() {
        dropTriggerIfPresent(
                "trg_procurement_audit_events_append_only"
        );

        dropTriggerIfPresent(
                "trg_procurement_audit_events_no_truncate"
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

        if (!Boolean.TRUE.equals(triggerExists)) {
            return;
        }

        String statement =
                "DROP TRIGGER "
                        + triggerName
                        + " ON procurement_audit_events";

        jdbcTemplate.execute(statement);
    }
}