package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "support_ticket_ai_assessments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ticket_ai_assessment_ticket_id",
                        columnNames = "ticket_id")
        },
        indexes = {
                @Index(
                        name = "idx_ticket_ai_assessment_ticket_id",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_ticket_ai_assessment_priority",
                        columnList = "priority"
                ),
                @Index(
                        name = "idx_ticket_ai_assessment_resolution_mode",
                        columnList = "resolution_mode"
                ),
                @Index(
                        name = "idx_ticket_ai_assessment_status",
                        columnList = "assessment_status"
                ),
                @Index(
                        name = "idx_ticket_ai_assessment_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketAiAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "assessment_reference",
            nullable = false,
            unique = true,
            length = 60)
    private String assessmentReference;

    @Column(name = "ticket_id",
            nullable = false,
            updatable = false)
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_category",
            nullable = false,
            length = 50)
    private DetectedCategory detectedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority",
            nullable = false,
            length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_mode",
            nullable = false,
            length = 30)
    private AiResolutionMode resolutionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_status",
            nullable = false,
            length = 30)
    private AiAssessmentStatus assessmentStatus;

    @Column(name = "summary",
            nullable = false,
            length = 1500)
    private String summary;

    @Column(name = "suggested_response",
            length = 4000)
    private String suggestedResponse;

    @Column(name = "escalation_reason",
            length = 1500)
    private String escalationReason;

    @Column(name = "confidence_score",
            nullable = false,
            precision = 5,
            scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "model_version",
            nullable = false,
            length = 100)
    private String modelVersion;

    @Column(name = "approved_response",
            length = 4000)
    private String approvedResponse;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

@Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason",
            length = 1500)
    private String rejectionReason;

    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (assessmentStatus == null) {
            assessmentStatus = AiAssessmentStatus.GENERATED;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    }

    public enum DetectedCategory {
        PLATFORM_SUPPORT,
        CLARIFICATION_REQUEST,
        COMPLIANCE_APPEAL,
        UPLOAD_ISSUE,
        BID_STATUS_QUERY,
        DOCUMENT_REQUIREMENT_QUERY,
        DEADLINE_QUERY,
        SCORING_DISPUTE,
        AWARD_DISPUTE,
        COMPLIANCE_OVERRIDE_REQUEST,
        CORRUPTION_ALLEGATION,
        GENERAL_SUPPORT,
        UNKNOWN
    }

    public enum TicketPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AiResolutionMode {
        AUTO_RESOLVE,
        DRAFT_FOR_APPROVAL,
        HUMAN_ONLY
    }

    public enum AiAssessmentStatus {
        GENERATED,
        APPROVED,
        EDITED_AND_APPROVED,
        RESPONSE_PUBLISHED,
        REJECTED,
        ESCALATED,
    }

}