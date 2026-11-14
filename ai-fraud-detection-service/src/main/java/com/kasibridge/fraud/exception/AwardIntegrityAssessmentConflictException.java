package com.kasibridge.fraud.exception;

public class AwardIntegrityAssessmentConflictException extends RuntimeException {
    public AwardIntegrityAssessmentConflictException(String message) {
        super(message);
    }

    public AwardIntegrityAssessmentConflictException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }

    public static AwardIntegrityAssessmentConflictException invalidTransition(String assessmentReference,
                                                                              String currentStatus,
                                                                              String requestedAction
    ) {
        return new
                AwardIntegrityAssessmentConflictException(
                "Award integrity assessment "
                        + assessmentReference
                        + " cannot perform action "
                        + requestedAction
                        + " while its current "
                        + "review status is "
                        + currentStatus
                        + "."
        );
    }
}
