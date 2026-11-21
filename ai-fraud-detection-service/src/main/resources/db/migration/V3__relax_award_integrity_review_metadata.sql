ALTER TABLE fraud.award_integrity_assessments
    DROP CONSTRAINT
        ck_award_integrity_review_metadata;

ALTER TABLE fraud.award_integrity_assessments
    ADD CONSTRAINT
        ck_award_integrity_review_metadata
    CHECK (
        (
            review_status = 'PENDING_REVIEW'
            AND reviewed_by IS NULL
            AND review_notes IS NULL
            AND reviewed_at IS NULL
        )
        OR
        (
            review_status = 'UNDER_REVIEW'
            AND reviewed_by IS NOT NULL
            AND reviewed_at IS NOT NULL
        )
        OR
        (
            review_status = 'ACCEPTED_WITH_MONITORING'
            AND (
                (
                    reviewed_by IS NULL
                    AND review_notes IS NULL
                    AND reviewed_at IS NULL
                )
                OR
                (
                    reviewed_by IS NOT NULL
                    AND review_notes IS NOT NULL
                    AND reviewed_at IS NOT NULL
                )
            )
        )
        OR
        (
            review_status IN (
                'COMPETITION_REVIEW_REQUIRED',
                'ESCALATED',
                'DISMISSED'
            )
            AND reviewed_by IS NOT NULL
            AND review_notes IS NOT NULL
            AND reviewed_at IS NOT NULL
        )
    );
