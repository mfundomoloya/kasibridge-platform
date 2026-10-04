package com.kasibridge.procurement.exception;

public class TenderNotFoundException extends RuntimeException {
    public TenderNotFoundException(String message) {
        super(message);
    }
}
