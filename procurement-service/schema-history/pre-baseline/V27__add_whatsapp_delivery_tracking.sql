ALTER TABLE notification_outbox
    ADD COLUMN delivery_status VARCHAR(30),
    ADD COLUMN provider_status_timestamp TIMESTAMP,
    ADD COLUMN delivered_at TIMESTAMP,
    ADD COLUMN provider_read_at TIMESTAMP,
    ADD COLUMN provider_failure_code VARCHAR(100),
    ADD COLUMN provider_failure_reason VARCHAR(1000);

CREATE INDEX idx_notification_provider_message_id
    ON notification_outbox (provider_message_id);

ALTER TABLE notification_outbox
    ADD CONSTRAINT notification_outbox_delivery_status_check
        CHECK (
            delivery_status IS NULL
                OR delivery_status IN (
                                       'ACCEPTED',
                                       'SENT',
                                       'DELIVERED',
                                       'READ',
                                       'FAILED'
                )
            );