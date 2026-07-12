package com.kasibridge.trader_profile.exception;

import java.util.Map;

public class BusinessValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public BusinessValidationException(String message, Map<String, String> fieldErrors)
    {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
