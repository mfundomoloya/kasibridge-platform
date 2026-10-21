CREATE UNIQUE INDEX
    uk_notification_bid_event_recipient
    ON public.notification_outbox (
                                   template_type,
                                   related_bid_id,
                                   recipient_user_id
        )
    WHERE related_bid_id IS NOT NULL
  AND recipient_user_id IS NOT NULL
  AND template_type IN (
      'BID_RECEIVED',
      'BID_COMPLIANCE_PASSED',
      'BID_COMPLIANCE_FAILED',
      'BID_WITHDRAWN',
      'TENDER_AWARDED'
  );