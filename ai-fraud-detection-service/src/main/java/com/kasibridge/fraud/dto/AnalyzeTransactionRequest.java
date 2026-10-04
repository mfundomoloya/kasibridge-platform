package com.kasibridge.fraud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request to analyze a transaction for fraud")
@Data
public class AnalyzeTransactionRequest {

    @Schema(example = "1")
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;

    @Schema(example = "100")
    @NotNull(message = "Trader ID is required")
    private Long traderId;

    @Schema(example = "60000")
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @Schema(example = "SALE")
    @NotNull(message = "Transaction type is required")
    private TransactionEvent.TransactionType type;

    @Schema(example = "CASH")
    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    @Schema(example = "2026-01-01T10:00:00")
    @NotNull(message = "occurredAt is required")
    private LocalDateTime occurredAt;

    @Schema(example = "2026-01-01T12:30:00")
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
