package com.kasibridge.fraud.dto;

public record AwardIntegrityAssessmentResult(

        PersistedAwardIntegrityAssessmentResponse assessment,

        boolean created
) {

    public AwardIntegrityAssessmentResult {
        if (assessment == null) {
            throw new IllegalArgumentException(
                    "Persisted award integrity "
                            + "assessment is required."
            );
        }
    }

    public static AwardIntegrityAssessmentResult created(PersistedAwardIntegrityAssessmentResponse assessment
    ) {
        return new AwardIntegrityAssessmentResult(
                assessment,
                true
        );
    }

    public static AwardIntegrityAssessmentResult existing(PersistedAwardIntegrityAssessmentResponse assessment
    ) {
        return new AwardIntegrityAssessmentResult(
                assessment,
                false
        );
    }
}