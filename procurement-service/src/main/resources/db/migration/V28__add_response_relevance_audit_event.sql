ALTER TABLE bids
    DROP CONSTRAINT IF EXISTS bids_status_check;

ALTER TABLE bids
    ADD CONSTRAINT bids_status_check
        CHECK (
            status IN (
                       'SUBMITTED',
                       'COMPLIANCE_FAILED',
                       'COMPLIANT',
                       'UNDER_EVALUATION',
                       'RECOMMENDED',
                       'REJECTED',
                       'AWARDED',
                       'WITHDRAWN'
                )
            );