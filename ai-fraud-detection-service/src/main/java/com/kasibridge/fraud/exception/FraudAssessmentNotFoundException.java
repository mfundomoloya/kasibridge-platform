package com.kasibridge.fraud.exception;

public class FraudAssessmentNotFoundException extends RuntimeException {
    public FraudAssessmentNotFoundException(String message) {
        super(message);
    }


    public FraudAssessmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
