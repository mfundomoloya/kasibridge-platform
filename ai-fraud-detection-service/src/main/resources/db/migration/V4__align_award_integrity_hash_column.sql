ALTER TABLE fraud.award_integrity_assessments
    ALTER COLUMN request_snapshot_sha256
    TYPE VARCHAR(64)
    USING BTRIM(request_snapshot_sha256);
