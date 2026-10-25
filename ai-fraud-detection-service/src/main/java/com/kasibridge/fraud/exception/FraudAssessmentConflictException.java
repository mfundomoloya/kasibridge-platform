package com.kasibridge.fraud.exception;

public class FraudAssessmentConflictException extends RuntimeException {
    public FraudAssessmentConflictException(String message) {
        super(message);
    }

    public FraudAssessmentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
