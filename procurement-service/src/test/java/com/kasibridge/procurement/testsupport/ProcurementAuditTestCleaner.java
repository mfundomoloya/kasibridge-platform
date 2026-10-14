package com.kasibridge.procurement.testsupport;

import com.kasibridge.procurement.config.ProcurementAuditImmutabilityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProcurementAuditTestCleaner {

    private final JdbcTemplate jdbcTemplate;

    private final ProcurementAuditImmutabilityConfig
            immutabilityConfig;

    @Transactional
    @SuppressWarnings({
            "SqlResolve",
            "SqlDialectInspection",
            "SqlNoDataSourceInspection"
    })
    public void clearAuditEvents() {
        immutabilityConfig
                .removeAuditImmutabilityProtection();

        try {
            jdbcTemplate.execute(
                    """
                    TRUNCATE TABLE
                        procurement_audit_events
                    RESTART IDENTITY
                    """
            );
        } finally {
            immutabilityConfig
                    .installAuditImmutabilityProtection();
        }
    }
}