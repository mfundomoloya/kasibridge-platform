package com.kasibridge.fraud.dto;

public record FraudAssessmentResult(FraudAssessmentResponse assessment, boolean created
) {

    public FraudAssessmentResult {
        if (assessment == null) {
            throw new IllegalArgumentException(
                    "Fraud assessment response "
                            + "is required."
            );
        }
    }

    public static FraudAssessmentResult created(FraudAssessmentResponse assessment) {
        return new FraudAssessmentResult(
                assessment,
                true
        );
    }

    public static FraudAssessmentResult existing(FraudAssessmentResponse assessment) {
        return new FraudAssessmentResult(
                assessment,
                false
        );
    }
}