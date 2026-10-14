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
            "SqlDialectInspection"
    })
    public void clearAuditEvents() {
        jdbcTemplate.queryForObject(
                """
                SELECT set_config(
                    'kasibridge.audit_maintenance',
                    'enabled',
                    true
                )
                """,
                String.class
        );

        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    procurement_audit_events
                RESTART IDENTITY
                """
        );
    }
}