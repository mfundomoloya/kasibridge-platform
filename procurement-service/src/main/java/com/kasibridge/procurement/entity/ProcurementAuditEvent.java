package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
        name = "procurement_audit_events",
        indexes = {
                @Index(name = "idx_audit_tender_id", columnList = "tender_id"),
                @Index(name = "idx_audit_bid_id", columnList = "bid_id"),
                @Index(name = "idx_audit_actor_user_id", columnList = "actor_user_id"),
                @Index(name = "idx_audit_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 80)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(name = "tender_id")
    private Long tenderId;

    @Column(name = "bid_id")
    private Long bidId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "result", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AuditResult result;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "details", length = 3000)
    private String details;

    @Column(name = "previous_event_hash", length = 128)
    private String previousEventHash;

    @Column(name = "event_hash", nullable = false, length = 128)
    private String eventHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if(createdAt == null) {
            createdAt = LocalDateTime.now()
                    .truncatedTo(ChronoUnit.MICROS);
            }
        }

    public enum AuditResult {
        SUCCESS,
        FAILED
    }

    public enum AuditEventType {
        TENDER_CREATED,
        TENDER_PUBLISHED,
        COMMITTEE_MEMBER_ASSIGNED,
        COMMITTEE_ASSIGNMENT_REJECTED_SOD,
        BID_SUBMITTED,
        BID_COMPLIANCE_PASSED,
        BID_COMPLIANCE_FAILED,
        BID_SCORE_VIEWED,
        BID_SCORE_SUBMITTED,
        BID_SCORE_REJECTED_DUPLICATE,
        BID_SCORE_REJECTED_UNASSIGNED_EVALUATOR,
        BID_SCORE_REJECTED_INVALID_TENDER_STATUS,
        TENDER_BIDDING_CLOSED,
        TENDER_EVALUATION_STARTED,
        TENDER_ADJUDICATION_SUMMARY_VIEWED,
        TENDER_ADJUDICATION_STARTED,
        TENDER_AWARDED,
        TENDER_AWARD_REJECTED,
        TENDER_SPECIFICATION_VERIFIED,
        TENDER_SPECIFICATION_TAMPER_DETECTED,
        PROCUREMENT_ANOMALY_DETECTED,
        PROCUREMENT_ANOMALY_REVIEWED,
        PROCUREMENT_ANOMALY_DISMISSED,
        PROCUREMENT_ANOMALY_TRANSITION_REJECTED,
        SUPPORT_TICKET_CREATED,
        SUPPORT_TICKET_RESPONDED,
        SUPPORT_TICKET_CLOSED,
        OFFICIAL_CLARIFICATION_PUBLISHED
    }
}