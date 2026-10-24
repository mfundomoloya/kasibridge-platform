package com.kasibridge.fraud.exception;

public class MlInferenceException extends RuntimeException {
    public MlInferenceException(String message) {
        super(message);
    }

    public MlInferenceException(String message, Throwable cause
    ) {
        super(message, cause);
    }
}
