package com.kasibridge.fraud.exception;

public class AwardIntegrityAssessmentNotFoundException extends RuntimeException {
    public AwardIntegrityAssessmentNotFoundException(String message) {
        super(message);
    }

    public AwardIntegrityAssessmentNotFoundException(String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}
