ALTER TABLE notification_outbox
DROP CONSTRAINT IF EXISTS notification_outbox_template_type_check;

ALTER TABLE notification_outbox
    ADD CONSTRAINT notification_outbox_template_type_check
        CHECK (
            template_type IN (
                              'SUPPORT_TICKET_CREATED',
                              'SUPPORT_TICKET_RESPONDED',
                              'SUPPORT_TICKET_CLOSED',
                              'OFFICIAL_CLARIFICATION_PUBLISHED',
                              'BID_RECEIVED',
                              'BID_COMPLIANCE_PASSED',
                              'BID_COMPLIANCE_FAILED',
                              'BID_WITHDRAWN',
                              'TENDER_AWARDED',
                              'PROCUREMENT_ANOMALY_DETECTED'
                )
            );