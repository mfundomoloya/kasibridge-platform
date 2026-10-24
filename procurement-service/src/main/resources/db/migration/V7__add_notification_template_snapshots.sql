ALTER TABLE public.notification_outbox
    ADD COLUMN template_recipient_name
        character varying(150);

ALTER TABLE public.notification_outbox
    ADD COLUMN template_tender_reference
        character varying(40);

ALTER TABLE public.notification_outbox
    ADD COLUMN template_tender_title
        character varying(150);