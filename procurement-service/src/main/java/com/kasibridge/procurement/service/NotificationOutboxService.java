package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationOutboxService {
    NotificationOutboxResponse queueNotification(
            NotificationOutbox.NotificationChannel channel,
            NotificationOutbox.NotificationTemplateType templateType,
            Long recipientUserId,
            String recipientPhone,
            String recipientEmail,
            String message,
            Long relatedTenderId,
            Long relatedBidId,
            Long relatedTicketId);

    Page<NotificationOutboxResponse> getNotifications(Pageable pageable);

    Page<NotificationOutboxResponse> getNotificationsByStatus(NotificationOutbox.NotificationStatus status, Pageable pageable);

    NotificationOutboxResponse getNotificationById(Long id);

    NotificationOutboxResponse markSent(Long id);

    NotificationOutboxResponse markFailed(Long id, MarkNotificationFailedRequest request);
}
