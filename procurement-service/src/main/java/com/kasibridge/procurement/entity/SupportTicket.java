package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "support_tickets",
        indexes = {
                @Index(name = "idx_ticket_reference", columnList = "ticket_reference"),
                @Index(name = "idx_ticket_tender_id", columnList = "tender_id"),
                @Index(name = "idx_ticket_bid_id", columnList = "bid_id"),
                @Index(name = "idx_ticket_created_by_user_id", columnList = "created_by_user_id"),
                @Index(name = "idx_ticket_trader_id", columnList = "trader_id"),
                @Index(name = "idx_ticket_contact_phone", columnList = "contact_phone_number"),
                @Index(name = "idx_ticket_business_name", columnList = "business_name_snapshot"),
                @Index(name = "idx_ticket_type", columnList = "ticket_type"),
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_support_ticket_assigned_to_user_id", columnList = "assigned_to_user_id"),
                @Index(name = "idx_support_ticket_assignment_source", columnList = "assignment_source")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_reference", nullable = false, unique = true, length = 60)

    private String ticketReference;
    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "bid_id")
    private Long bidId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "trader_id")
    private Long traderId;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_phone_number", length = 30)
    private String contactPhoneNumber;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "business_name_snapshot", length = 150)
    private String businessNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 40)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description", nullable = false, length = 3000)
    private String description;

    @Column(name = "response", length = 3000)
    private String response;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "assignment_reason", length = 1000)
    private String assignmentReason;

    @Column(name = "assignment_source", length = 30)
    @Enumerated(EnumType.STRING)
    private AssignmentSource assignmentSource;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "review_started_at")
    private LocalDateTime reviewStartedAt;

    @Column(name = "responded_by_user_id")
    private Long respondedByUserId;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closure_notes", length = 1000)
    private String closureNotes;

    @Column(name = "public_clarification", nullable = false)
    private boolean publicClarification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = TicketStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TicketType {
        CLARIFICATION_REQUEST,
        COMPLIANCE_APPEAL,
        UPLOAD_ISSUE,
        GENERAL_SUPPORT
    }

    public enum TicketStatus {
        OPEN,
        IN_REVIEW,
        RESPONDED,
        CLOSED,
        REJECTED
    }

    public enum AssignmentSource {
        MANUAL,
        AI_ROUTING,
        SYSTEM_FALLBACK
    }
}
