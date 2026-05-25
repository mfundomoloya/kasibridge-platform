package com.kasibridge.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                //core query performance
                @Index(name = "idx_transaction_trader_id", columnList = "trader_id"),
                @Index(name = "idx_transaction_trader_occurred", columnList = "trader_id, occurred_at"),

                //for analytics and credibility
                @Index(name = "idx_transaction_trader_type_status", columnList = "trader_id, type, status"),
                @Index(name = "idx_transaction_trader_channel", columnList = "trader_id, channel"),

                //time based queries
                @Index(name = "idx_transaction_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_transaction_recorded_at", columnList = "recorded_at"),

                //lookup / filtering
                @Index(name = "idx_transaction_reference", columnList = "reference_number"),
                @Index(name = "idx_transaction_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trader_id", nullable = false, updatable = false)
    private Long traderId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, updatable = false, length = 3)

    private Currency currency = Currency.ZAR;

    @Column(name = "description", length = 255, updatable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, updatable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 20)
    private TransactionChannel channel;

    @Column(name = "customer_name", length = 120, updatable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 30, updatable = false)
    private String customerPhone;


    //ref number
    @Column(name = "reference_number", nullable = false, unique = true,length = 30, updatable = false)
    private String referenceNumber;

    //mutable full
    @Column(name = "notes", length = 500)
    private String notes;

    //time model
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    // status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    // lock for future
    @Version
    @Column(name = "version")
    private Long version;

    //audit
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //lifecycle
    @PrePersist
    protected void onCreate() {

        // Fail fast for required fields (gives clearer errors than DB constraint failures)
        if (traderId == null) throw new IllegalStateException("traderId must be provided");
        if (amount == null) throw new IllegalStateException("amount must be provided");
        if (type == null) throw new IllegalStateException("type must be provided");
        if (paymentMethod == null) throw new IllegalStateException("paymentMethod must be provided");

        if (currency == null) {
            currency = Currency.ZAR;
        }

        if (channel == null) {
            channel = TransactionChannel.WEB;
        }

        if (referenceNumber == null || referenceNumber.isBlank()) {
            referenceNumber = "KB-" + UUID.randomUUID()
                    .toString()
                    .toUpperCase()
                    .replace("-", "")
                    .substring(0, 12);
        }

        recordedAt = LocalDateTime.now();

        if (occurredAt == null) {
            occurredAt = recordedAt;
        }

        // Intelligent default status
        if (status == null) {
            status = (paymentMethod == PaymentMethod.CASH)
                    ? TransactionStatus.COMPLETED
                    : TransactionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    //controlled mutators
    public void updateNotes(String notes) {
        this.notes = notes;
    }

    public void changeStatus(TransactionStatus status) {
        this.status = status;
    }

    //this entity needs a factory method since it does not have setters, it will be needed for the service implementation
    public static Transaction create(
            Long traderId,
            BigDecimal amount,
            TransactionType type,
            PaymentMethod paymentMethod,
            TransactionChannel channel,
            String description,
            String customerName,
            String customerPhone,
            LocalDateTime occurredAt,
            String notes
    ){
        Transaction t = new Transaction();
        t.traderId = traderId;
        t.amount = amount;
        t.type = type;
        t.paymentMethod = paymentMethod;
        t.channel = channel;
        t.description = description;
        t.customerName = customerName;
        t.customerPhone = customerPhone;
        t.occurredAt = occurredAt;
        t.notes = notes;
        // currency, referenceNumber, recordedAt, status
        // all handled by @PrePersist
        return t;   
    }


    // enums
    public enum TransactionType {
        SALE,
        EXPENSE,
        REFUND
    }

    public enum PaymentMethod {
        CASH,
        EFT,
        QR,
        WHATSAPP
    }

    public enum TransactionChannel {
        WEB,
        WHATSAPP
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        CANCELLED,
        FLAGGED
    }

    public enum Currency {
        ZAR
    }
}
