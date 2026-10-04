BEGIN;

ALTER TABLE public.procurement_audit_events
DROP CONSTRAINT IF EXISTS
        procurement_audit_events_event_type_check;

ALTER TABLE public.procurement_audit_events
    ADD CONSTRAINT procurement_audit_events_event_type_check
        CHECK (
            event_type IN (
                           'TENDER_CREATED',
                           'TENDER_PUBLISHED',
                           'TENDER_BIDDING_CLOSED',
                           'TENDER_EVALUATION_STARTED',
                           'TENDER_ADJUDICATION_STARTED',
                           'TENDER_SPECIFICATION_VERIFIED',
                           'TENDER_SPECIFICATION_TAMPER_DETECTED',

                           'COMMITTEE_MEMBER_ASSIGNED',
                           'COMMITTEE_ASSIGNMENT_REJECTED_SOD',

                           'BID_SUBMITTED',
                           'BID_COMPLIANCE_PASSED',
                           'BID_COMPLIANCE_FAILED',
                           'BID_SCORE_VIEWED',
                           'BID_SCORE_SUBMITTED',
                           'BID_SCORE_REJECTED_DUPLICATE',
                           'BID_SCORE_REJECTED_UNASSIGNED_EVALUATOR',
                           'BID_SCORE_REJECTED_INVALID_TENDER_STATUS',

                           'TENDER_ADJUDICATION_SUMMARY_VIEWED',
                           'TENDER_AWARDED',
                           'TENDER_AWARD_REJECTED',

                           'PROCUREMENT_ANOMALY_DETECTED',
                           'PROCUREMENT_ANOMALY_REVIEWED',
                           'PROCUREMENT_ANOMALY_DISMISSED',
                           'PROCUREMENT_ANOMALY_TRANSITION_REJECTED',

                           'SUPPORT_TICKET_CREATED',
                           'SUPPORT_TICKET_ASSIGNED',
                           'SUPPORT_TICKET_REASSIGNED',
                           'SUPPORT_TICKET_ASSIGNMENT_REJECTED',
                           'SUPPORT_TICKET_REASSIGNMENT_BLOCKED',
                           'SUPPORT_TICKET_REVIEW_STARTED',
                           'SUPPORT_TICKET_RETURNED_TO_QUEUE',
                           'SUPPORT_TICKET_RESPONDED',
                           'SUPPORT_TICKET_REJECTED',
                           'SUPPORT_TICKET_CLOSED',
                           'OFFICIAL_CLARIFICATION_PUBLISHED',

                           'SUPPORT_TICKET_AI_ASSESSMENT_CREATED',
                           'SUPPORT_TICKET_AI_ASSESSMENT_APPROVED',
                           'SUPPORT_TICKET_RESPONSE_RELEVANCE_CONFIRMED',
                           'SUPPORT_TICKET_AI_ASSESSMENT_REJECTED',
                           'SUPPORT_TICKET_AI_ASSESSMENT_ESCALATED',
                           'SUPPORT_TICKET_AI_RESPONSE_PUBLISHED'
                )
            );

COMMIT;