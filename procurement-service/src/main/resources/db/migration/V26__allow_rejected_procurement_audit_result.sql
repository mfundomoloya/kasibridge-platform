ALTER TABLE procurement_audit_events
DROP CONSTRAINT IF EXISTS procurement_audit_events_result_check;

ALTER TABLE procurement_audit_events
    ADD CONSTRAINT procurement_audit_events_result_check
        CHECK (
            result IN (
                       'SUCCESS',
                       'FAILURE',
                       'REJECTED'
                )
            );