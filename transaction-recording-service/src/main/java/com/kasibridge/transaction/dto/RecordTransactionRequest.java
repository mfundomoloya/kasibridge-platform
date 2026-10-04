package com.kasibridge.transaction.dto;

import com.kasibridge.transaction.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecordTransactionRequest {

    @NotNull(message = "Trader ID is required")
    private Long traderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private Transaction.TransactionType type;

    @NotNull(message = "Payment method is required")
    private Transaction.PaymentMethod paymentMethod;

    //this will default to WEB if not provided
    private Transaction.TransactionChannel channel;


    //this is optional because many POS/bank-sync transactions won't have one
    @Size(max = 255, message = "Description must be 255 characters or less")
    private String description;

    ///customer info
    @Size(max = 120, message = "customerName must be 120 characters or less")
    private String customerName;


    @Size(max = 30, message = "customerPhone must be 30 characters or less")
    @Pattern(
            regexp = "^(\\+\\d{1,3})?\\d{7,15}$",
            message = "customerPhone must be a valid phone number"
    )
    private String customerPhone;

    //when the transaction happened
    //if not provided, defaults to recordedAt (system time)
    //used for offline/late capture
    private LocalDateTime occurredAt;

    //notes are optional
    @Size(max = 500, message = "notes must be 500 characters or less")
    private String notes;
}
