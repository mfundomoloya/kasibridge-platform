ALTER TABLE public.notification_outbox
DROP CONSTRAINT
        notification_outbox_template_type_check;

ALTER TABLE public.notification_outbox
    ADD CONSTRAINT
        notification_outbox_template_type_check
        CHECK (
            template_type IN (
                              'SUPPORT_TICKET_CREATED',
                              'SUPPORT_TICKET_RESPONDED',
                              'SUPPORT_TICKET_CLOSED',
                              'OFFICIAL_CLARIFICATION_PUBLISHED',
                              'BID_RECEIVED',
                              'BID_COMPLIANCE_PASSED',
                              'BID_COMPLIANCE_FAILED',
                              'MATCHING_TENDER_PUBLISHED',
                              'TENDER_AWARDED',
                              'PROCUREMENT_ANOMALY_DETECTED',
                              'BID_WITHDRAWN'
                )
            );

CREATE UNIQUE INDEX
    uk_notification_matching_tender_recipient
    ON public.notification_outbox (
                                   template_type,
                                   related_tender_id,
                                   recipient_user_id
        )
    WHERE related_tender_id IS NOT NULL
  AND recipient_user_id IS NOT NULL
  AND template_type =
      'MATCHING_TENDER_PUBLISHED';