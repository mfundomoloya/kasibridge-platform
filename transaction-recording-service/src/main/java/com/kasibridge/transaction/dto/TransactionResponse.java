package com.kasibridge.transaction.dto;

import com.kasibridge.transaction.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private Long traderId;
    private BigDecimal amount;
    private Transaction.Currency currency;
    private String description;
    private Transaction.TransactionType type;
    private Transaction.PaymentMethod paymentMethod;
    private Transaction.TransactionChannel channel;
    private String customerName;
    private String customerPhone;
    private String referenceNumber;
    private String notes;
    private Transaction.TransactionStatus status;

    // Both timestamps exposed — gap flags late/offline entries
    private LocalDateTime occurredAt;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;

    // Derived field — how many minutes between occurredAt and recordedAt
    // Useful for fraud detection: large gaps = suspicious backdating
    private Long captureDelayMinutes;

    public static TransactionResponse from(Transaction transaction) {
        Long delayMinutes = null;

        if (transaction.getOccurredAt() != null && transaction.getRecordedAt() != null) {
            delayMinutes = Math.abs(
                    Duration.between(
                    transaction.getOccurredAt(),
                    transaction.getRecordedAt()
                ).toMinutes()
            );
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .traderId(transaction.getTraderId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .type(transaction.getType())
                .paymentMethod(transaction.getPaymentMethod())
                .channel(transaction.getChannel())
                .customerName(transaction.getCustomerName())
                .customerPhone(transaction.getCustomerPhone())
                .referenceNumber(transaction.getReferenceNumber())
                .notes(transaction.getNotes())
                .status(transaction.getStatus())
                .occurredAt(transaction.getOccurredAt())
                .recordedAt(transaction.getRecordedAt())
                .updatedAt(transaction.getUpdatedAt())
                .captureDelayMinutes(delayMinutes)
                .build();
    }
}
