package com.kasibridge.procurement.exception;

public class NotificationOutboxException extends RuntimeException {
    public NotificationOutboxException(String message) {
        super(message);
    }
}
