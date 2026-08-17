package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_outbox",
        indexes = {
                @Index(name = "idx_notification_channel", columnList = "channel"),
                @Index(name = "idx_notification_status", columnList = "status"),
                @Index(name = "idx_notification_recipient_user_id", columnList = "recipient_user_id"),
                @Index(name = "idx_notification_tender_id", columnList = "related_tender_id"),
                @Index(name = "idx_notification_bid_id", columnList = "related_bid_id"),
                @Index(name = "idx_notification_ticket_id", columnList = "related_ticket_id"),
                @Index(name = "idx_notification_created_at", columnList = "created_at")}
        )

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_reference", nullable = false, unique = true, length = 60)
    private String notificationReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 60)
    private NotificationTemplateType templateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Column(name = "message", nullable = false, length = 3000)
    private String message;

    @Column(name = "related_tender_id")
    private Long relatedTenderId;

    @Column(name = "related_bid_id")
    private Long relatedBidId;

    @Column(name = "related_ticket_id")
    private Long relatedTicketId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();

    }

    public enum NotificationChannel {
        WHATSAPP,
        EMAIL,
        SMS,
        IN_APP
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        CANCELLED
    }

    public enum NotificationTemplateType {
        SUPPORT_TICKET_CREATED,
        SUPPORT_TICKET_RESPONDED,
        SUPPORT_TICKET_CLOSED,
        OFFICIAL_CLARIFICATION_PUBLISHED,
        BID_RECEIVED,
        BID_COMPLIANCE_PASSED,
        BID_COMPLIANCE_FAILED,
        TENDER_AWARDED,
        PROCUREMENT_ANOMALY_DETECTED
    }
}
