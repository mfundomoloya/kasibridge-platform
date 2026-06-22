package com.kasibridge.fraud.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents transaction data pulled from the Transaction Recording Service.
 * Used as input to the fraud detection rules engine.
 */
public record TransactionEvent(
        Long transactionId,
        Long traderId,
        BigDecimal amount,
        TransactionType type,
        String paymentMethod,
        LocalDateTime occurredAt,
        LocalDateTime recordedAt
) {
    public enum TransactionType{
        SALE,
        EXPENSE,
        REFUND
    }
}
