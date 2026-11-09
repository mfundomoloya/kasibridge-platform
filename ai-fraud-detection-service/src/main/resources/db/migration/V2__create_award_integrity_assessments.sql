CREATE TABLE fraud.award_integrity_assessments (
                                                   id BIGSERIAL
                                                       PRIMARY KEY,

                                                   assessment_reference VARCHAR(40)
                                                       NOT NULL,

                                                   tender_id BIGINT
                                                       NOT NULL,

                                                   assessment_type VARCHAR(60)
                                                       NOT NULL,

                                                   risk_score NUMERIC(10, 8)
                                                       NOT NULL,

                                                   risk_level VARCHAR(20)
                                                       NOT NULL,

                                                   recommendation VARCHAR(60)
                                                       NOT NULL,

                                                   rule_version VARCHAR(100)
                                                       NOT NULL,

                                                   model_used BOOLEAN
                                                       NOT NULL,

                                                   award_consistent_with_rules BOOLEAN
                                                       NOT NULL,

                                                   request_snapshot_json TEXT
                                                       NOT NULL,

                                                   request_snapshot_sha256 CHAR(64)
                                                       NOT NULL,

                                                   signals_json TEXT
                                                       NOT NULL,

                                                   review_status VARCHAR(40)
                                                       NOT NULL,

                                                   reviewed_by VARCHAR(100),

                                                   review_notes VARCHAR(1000),

                                                   reviewed_at TIMESTAMP,

                                                   assessed_at TIMESTAMP
                                                       NOT NULL,

                                                   created_at TIMESTAMP
                                                       NOT NULL,

                                                   updated_at TIMESTAMP,

                                                   version BIGINT
                                                       NOT NULL
                                                       DEFAULT 0,

                                                   CONSTRAINT uk_award_integrity_reference
                                                       UNIQUE (
                                                               assessment_reference
                                                           ),

                                                   CONSTRAINT uk_award_integrity_evidence
                                                       UNIQUE (
                                                               tender_id,
                                                               rule_version,
                                                               request_snapshot_sha256
                                                           ),

                                                   CONSTRAINT ck_award_integrity_tender_id
                                                       CHECK (
                                                           tender_id > 0
                                                           ),

                                                   CONSTRAINT ck_award_integrity_assessment_type
                                                       CHECK (
                                                           assessment_type
                                                               = 'DETERMINISTIC_AWARD_INTEGRITY'
                                                           ),

                                                   CONSTRAINT ck_award_integrity_risk_score
                                                       CHECK (
                                                           risk_score >= 0
                                                               AND risk_score <= 1
                                                           ),

                                                   CONSTRAINT ck_award_integrity_risk_level
                                                       CHECK (
                                                           risk_level IN (
                                                                          'LOW',
                                                                          'MEDIUM',
                                                                          'HIGH',
                                                                          'CRITICAL'
                                                               )
                                                           ),

                                                   CONSTRAINT ck_award_integrity_recommendation
                                                       CHECK (
                                                           recommendation IN (
                                                                              'ACCEPT_WITH_MONITORING',
                                                                              'REVIEW_COMPETITION',
                                                                              'REQUIRE_GOVERNANCE_REVIEW',
                                                                              'ESCALATE_FOR_INVESTIGATION'
                                                               )
                                                           ),

                                                   CONSTRAINT ck_award_integrity_review_status
                                                       CHECK (
                                                           review_status IN (
                                                                             'PENDING_REVIEW',
                                                                             'UNDER_REVIEW',
                                                                             'ACCEPTED_WITH_MONITORING',
                                                                             'COMPETITION_REVIEW_REQUIRED',
                                                                             'ESCALATED',
                                                                             'DISMISSED'
                                                               )
                                                           ),

                                                   CONSTRAINT ck_award_integrity_snapshot_json
                                                       CHECK (
                                                           LENGTH(
                                                                   TRIM(
                                                                           request_snapshot_json
                                                                   )
                                                           ) > 0
                                                           ),

                                                   CONSTRAINT ck_award_integrity_signals_json
                                                       CHECK (
                                                           LENGTH(
                                                                   TRIM(
                                                                           signals_json
                                                                   )
                                                           ) > 0
                                                           ),

                                                   CONSTRAINT ck_award_integrity_snapshot_hash
                                                       CHECK (
                                                           request_snapshot_sha256
                                                           ~ '^[0-9a-f]{64}$'
),

    CONSTRAINT ck_award_integrity_reference_format
        CHECK (
            assessment_reference
                ~ '^KB-AWARD-RISK-[A-Z0-9]{8,20}$'
        ),

    CONSTRAINT ck_award_integrity_review_metadata
        CHECK (
            (
                review_status = 'PENDING_REVIEW'
                AND reviewed_by IS NULL
                AND reviewed_notes IS NULL
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
                review_status IN (
                    'ACCEPTED_WITH_MONITORING',
                    'COMPETITION_REVIEW_REQUIRED',
                    'ESCALATED',
                    'DISMISSED'
                )
                AND reviewed_by IS NOT NULL
                AND reviewed_notes IS NOT NULL
                AND reviewed_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_award_integrity_tender
    ON fraud.award_integrity_assessments (
                                          tender_id
        );

CREATE INDEX idx_award_integrity_risk
    ON fraud.award_integrity_assessments (
                                          risk_level
        );

CREATE INDEX idx_award_integrity_review
    ON fraud.award_integrity_assessments (
                                          review_status
        );

CREATE INDEX idx_award_integrity_queue
    ON fraud.award_integrity_assessments (
                                          review_status,
                                          risk_level,
                                          assessed_at
        );

CREATE INDEX idx_award_integrity_assessed_at
    ON fraud.award_integrity_assessments (
                                          assessed_at DESC
        );

CREATE INDEX idx_award_integrity_open_review
    ON fraud.award_integrity_assessments (
                                          risk_level,
                                          assessed_at
        )
    WHERE review_status IN (
        'PENDING_REVIEW',
        'UNDER_REVIEW',
        'ESCALATED'
    );

COMMENT ON TABLE
    fraud.award_integrity_assessments
IS
    'Immutable tender award-integrity evidence '
    'with a controlled governance review state.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .assessment_reference
IS
    'Public assessment reference generated in '
    'the format KB-AWARD-RISK-XXXXXXXX.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .assessment_type
IS
    'Assessment mechanism. The current supported '
    'value is DETERMINISTIC_AWARD_INTEGRITY.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .model_used
IS
    'False for deterministic award-integrity '
    'rules. The invoice fraud model is not used.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .request_snapshot_json
IS
    'Canonical JSON snapshot of the exact evidence '
    'submitted for the assessment.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .request_snapshot_sha256
IS
    'SHA-256 digest of the canonical request '
    'snapshot used for integrity and idempotency.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .signals_json
IS
    'Immutable JSON snapshot of the explainable '
    'signals returned by the rule service.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .review_status
IS
    'Mutable governance-review state. Changes do '
    'not alter the original assessment evidence.';

COMMENT ON COLUMN
    fraud.award_integrity_assessments
        .version
IS
    'Optimistic-lock value used to prevent '
    'conflicting analyst review updates.';