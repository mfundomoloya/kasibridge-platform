package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "procurement_anomalies",
        indexes = {
                @Index(name = "idx_anomaly_tender_id", columnList = "tender_id"),
                @Index(name = "idx_anomaly_bid_id", columnList = "bid_id"),
                @Index(name = "idx_anomaly_type", columnList = "type"),
                @Index(name = "idx_anomaly_status", columnList = "status"),
                @Index(name = "idx_anomaly_detected_at", columnList = "detected_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anomaly_reference", nullable = false, unique = true, length = 50)
    private String anomalyReference;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "bid_id")
    private Long bidId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 60)
    private AnomalyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnomalyStatus status;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "evidence", length = 3000)
    private String evidence;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();

        if (status == null) {
            status = AnomalyStatus.OPEN;
        }
    }

    public enum AnomalyType {
        HIGHEST_SCORE_BYPASS,
        RAPID_AWARD,
        LARGE_SCORE_GAP,
        SPECIFICATION_TAMPER_DETECTED,
        MANUAL_PRICE_SCORE_ATTEMPT
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AnomalyStatus {
        OPEN,
        REVIEWED,
        DISMISSED
    }
}