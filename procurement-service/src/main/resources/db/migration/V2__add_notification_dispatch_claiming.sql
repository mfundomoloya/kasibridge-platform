ALTER TABLE public.notification_outbox
    ADD COLUMN claimed_at
        TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN claimed_by
        VARCHAR(100),
    ADD COLUMN claim_expires_at
        TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN next_attempt_at
        TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN version
        BIGINT NOT NULL DEFAULT 0;

ALTER TABLE public.notification_outbox
DROP CONSTRAINT notification_outbox_status_check;

ALTER TABLE public.notification_outbox
    ADD CONSTRAINT notification_outbox_status_check
        CHECK (
            status IN (
                       'PENDING',
                       'PROCESSING',
                       'SENT',
                       'FAILED',
                       'CANCELLED'
                )
            );

CREATE INDEX idx_notification_dispatch_queue
    ON public.notification_outbox (
                                   channel,
                                   status,
                                   next_attempt_at,
                                   created_at,
                                   id
        )
    WHERE status IN (
        'PENDING',
        'PROCESSING',
        'FAILED'
    );

CREATE INDEX idx_notification_claim_expiry
    ON public.notification_outbox (
                                   claim_expires_at,
                                   id
        )
    WHERE status = 'PROCESSING';