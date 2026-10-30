package com.kasibridge.fraud.exception;

public class FraudAssessmentVersionConflictException extends RuntimeException {
    public FraudAssessmentVersionConflictException(String message) {
        super(message);
    }

    public FraudAssessmentVersionConflictException(String assessmentReference, Long expectedVersion, Long actualVersion) {
        super(
                "Fraud assessment "
                        + assessmentReference
                        + " was modified by another "
                        + "request. Expected version "
                        + expectedVersion
                        + ", but the current version is "
                        + actualVersion
                        + ". Refresh the assessment "
                        + "before submitting another "
                        + "review decision."
        );
    }

    public FraudAssessmentVersionConflictException(String message, Throwable cause) {
        super(
                message,
                cause
        );
    }
}
