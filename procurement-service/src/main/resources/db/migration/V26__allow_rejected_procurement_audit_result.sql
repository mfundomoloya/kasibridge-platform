ALTER TABLE support_ticket_ai_assessments
    ADD COLUMN IF NOT EXISTS
    response_relevance_confirmed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE support_ticket_ai_assessments
    ADD COLUMN IF NOT EXISTS
    response_relevance_confirmed_by_user_id BIGINT;

ALTER TABLE support_ticket_ai_assessments
    ADD COLUMN IF NOT EXISTS
    response_relevance_confirmed_at TIMESTAMP;

ALTER TABLE support_ticket_ai_assessments
    ADD COLUMN IF NOT EXISTS
    response_relevance_confirmation_notes VARCHAR(1000);