package com.kasibridge.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "fraud_alerts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fraud_transaction_pattern",
                                columnNames = {"transaction_id", "pattern_type"}
                )
        },
        indexes = {
                // Core lookups
                @Index(name = "idx_fraud_trader_id", columnList = "trader_id"),
                @Index(name = "idx_fraud_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_fraud_pattern_type", columnList = "pattern_type"),
                @Index(name = "idx_fraud_status", columnList = "status"),

                // Auditor dashboard queries
                @Index(name = "idx_fraud_trader_status", columnList = "trader_id, status"),
                @Index(name = "idx_fraud_trader_pattern", columnList = "trader_id, pattern_type"),

                // Time-based queries
                @Index(name = "idx_fraud_detected_at", columnList = "detected_at")

        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "alert_reference", nullable = false, unique = true, updatable = false, length = 30)
    private String alertReference;

    //Trader & Transaction
    @Column(name = "trader_id", nullable = false, updatable = false)
    private Long traderId;

    @Column(name = "transaction_id", updatable = false)
    private Long transactionId;

    //Pattern
    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type", nullable = false, updatable = false, length = 30)
    private FraudPatternType patternType;

    //Severity
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity;

    //Evidence
    @Column(name = "description", nullable = false, length = 1000, updatable = false)
    private String description;

    @Column(name = "evidence", columnDefinition = "TEXT", updatable = false)
    private String evidence;

    //Financial Context
    @Column(name = "transaction_amount", precision = 15, scale = 2, nullable = false, updatable = false)
    private BigDecimal transactionAmount;

    //Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    //Review Workflow
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    //Audit
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //Optimistic Locking
    @Version
    @Column(name = "version")
    private Long version;

    //Lifecycle
    @PrePersist
    protected void onCreate() {
        if (alertReference == null || alertReference.isBlank()) {
            alertReference = "KB-ALERT-" + UUID.randomUUID()
                    .toString()
                    .toUpperCase()
                    .replace("-", "")
                    .substring(0, 8);
        }

        detectedAt = LocalDateTime.now();

        if (status == null) {
            status = AlertStatus.OPEN;
        }

        if (severity == null) {
            severity = defaultSeverityFor(patternType);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    //Controlled Mutators
    public void review(String reviewedBy, String reviewNotes) {
        this.reviewedBy = reviewedBy;
        this.reviewNotes = reviewNotes;
        this.reviewedAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.status = AlertStatus.REVIEWED;
    }

    public void dismiss(String reviewedBy, String reviewNotes) {
        this.reviewedBy = reviewedBy;
        this.reviewNotes = reviewNotes;
        this.reviewedAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.status = AlertStatus.DISMISSED;
    }

    public void escalate() {
        this.severity = Severity.HIGH;
        this.status = AlertStatus.ESCALATED;
    }

    //Factory Method
    public static FraudAlert create(
            Long traderId,
            Long transactionId,
            FraudPatternType patternType,
            Severity severity,
            String description,
            String evidence,
            BigDecimal transactionAmount
    ) {
        FraudAlert alert = new FraudAlert();
        alert.traderId = traderId;
        alert.transactionId = transactionId;
        alert.patternType = patternType;
        alert.severity = severity;
        alert.description = description;
        alert.evidence = evidence;
        alert.transactionAmount = transactionAmount;
        return alert;
    }

    //Internal Helper
    private Severity defaultSeverityFor(FraudPatternType patternType) {
        if (patternType == null) {
            return Severity.MEDIUM;
        }

        return switch (patternType) {
            case HIGH_VALUE_TRANSACTION -> Severity.HIGH;
            case BACKDATED_TRANSACTION -> Severity.MEDIUM;
            case DUPLICATE_TRANSACTION -> Severity.MEDIUM;
        };
    }

    //Enums
    public enum FraudPatternType {
        BACKDATED_TRANSACTION,
        HIGH_VALUE_TRANSACTION,
        DUPLICATE_TRANSACTION
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum AlertStatus {
        OPEN,
        REVIEWED,
        DISMISSED,
        ESCALATED
    }

}
