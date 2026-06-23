package com.kasibridge.fraud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnalyzeTransactionRequest {

    @NotNull(message = "Transaction ID is required")
    private Long transactionId;

    @NotNull(message = "Trader ID is required")
    private Long traderId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionEvent.TransactionType type;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "occurredAt is required")
    private LocalDateTime occurredAt;

    @NotNull(message = "recordedAt is required")
    private LocalDateTime recordedAt;


    public TransactionEvent toTransactionEvent() {
        return new TransactionEvent(
                transactionId,
                traderId,
                amount,
                type,
                paymentMethod,
                occurredAt,
                recordedAt
        );
    }

}
