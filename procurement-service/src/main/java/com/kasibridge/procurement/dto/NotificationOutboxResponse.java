package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.NotificationOutbox;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationOutboxResponse {
    private Long id;
    private String notificationReference;
    private NotificationOutbox.NotificationChannel channel;
    private NotificationOutbox.NotificationTemplateType templateType;
    private NotificationOutbox.NotificationStatus status;

    private Long recipientUserId;
    private String recipientPhone;
    private String recipientEmail;

    private String message;

    private Long relatedTenderId;
    private Long relatedBidId;
    private Long relatedTicketId;

    private int retryCount;
    private String failureReason;
    private String providerMessageId;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;

    public static NotificationOutboxResponse from(NotificationOutbox notification) {

        return NotificationOutboxResponse.builder()
                .id(notification.getId())
                .notificationReference(notification.getNotificationReference())
                .channel(notification.getChannel())
                .templateType(notification.getTemplateType())
                .status(notification.getStatus())
                .recipientUserId(notification.getRecipientUserId())
                .recipientPhone(notification.getRecipientPhone())
                .recipientEmail(notification.getRecipientEmail())
                .message(notification.getMessage())
                .relatedTenderId(notification.getRelatedTenderId())
                .relatedBidId(notification.getRelatedBidId())
                .relatedTicketId(notification.getRelatedTicketId())
                .retryCount(notification.getRetryCount())
                .failureReason(notification.getFailureReason())
                .providerMessageId(notification.getProviderMessageId())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
