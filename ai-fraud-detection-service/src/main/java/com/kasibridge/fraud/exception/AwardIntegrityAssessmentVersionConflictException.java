package com.kasibridge.fraud.exception;

public class AwardIntegrityAssessmentVersionConflictException extends RuntimeException {
    public AwardIntegrityAssessmentVersionConflictException(String message) {
        super(message);
    }
    public AwardIntegrityAssessmentVersionConflictException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }

    public static
    AwardIntegrityAssessmentVersionConflictException forVersions(String assessmentReference,
                                                                 Long expectedVersion,
                                                                 Long actualVersion
    ) {
        return new
                AwardIntegrityAssessmentVersionConflictException(
                "Award integrity assessment "
                        + assessmentReference
                        + " was modified by another "
                        + "reviewer. Expected version "
                        + expectedVersion
                        + ", but the current version "
                        + "is "
                        + actualVersion
                        + ". Refresh the assessment "
                        + "before retrying."
        );
    }

}
