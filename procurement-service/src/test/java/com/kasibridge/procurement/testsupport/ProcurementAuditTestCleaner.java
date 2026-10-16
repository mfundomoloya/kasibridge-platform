package com.kasibridge.procurement.testsupport;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcurementAuditTestCleaner {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    public void clearAuditEvents() {
        removeAuditTriggers();

        try {
            jdbcTemplate.execute(
                    """
                    TRUNCATE TABLE
                        public.procurement_audit_events
                    RESTART IDENTITY
                    """
            );
        } finally {
            installAuditProtection();
        }
    }

    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    private void removeAuditTriggers() {
        jdbcTemplate.execute(
                """
                DROP TRIGGER IF EXISTS
                    trg_procurement_audit_events_append_only
                ON public.procurement_audit_events
                """
        );

        jdbcTemplate.execute(
                """
                DROP TRIGGER IF EXISTS
                    trg_procurement_audit_events_no_truncate
                ON public.procurement_audit_events
                """
        );
    }

    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    private void installAuditProtection() {
        jdbcTemplate.execute(
                """
                CREATE OR REPLACE FUNCTION
                    public.prevent_procurement_audit_mutation()
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

        removeAuditTriggers();

        jdbcTemplate.execute(
                """
                CREATE TRIGGER
                    trg_procurement_audit_events_append_only
                BEFORE UPDATE OR DELETE
                ON public.procurement_audit_events
                FOR EACH ROW
                EXECUTE FUNCTION
                    public.prevent_procurement_audit_mutation()
                """
        );

        jdbcTemplate.execute(
                """
                CREATE TRIGGER
                    trg_procurement_audit_events_no_truncate
                BEFORE TRUNCATE
                ON public.procurement_audit_events
                FOR EACH STATEMENT
                EXECUTE FUNCTION
                    public.prevent_procurement_audit_mutation()
                """
        );
    }
}