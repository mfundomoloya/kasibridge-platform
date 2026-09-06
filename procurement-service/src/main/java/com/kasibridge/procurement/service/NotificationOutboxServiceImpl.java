package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxServiceImpl implements  NotificationOutboxService {

    private final NotificationOutboxRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationOutboxResponse queueNotification(NotificationOutbox.NotificationChannel channel,
                                                        NotificationOutbox.NotificationTemplateType templateType,
                                                        Long recipientUserId,
                                                        String recipientPhone,
                                                        String recipientEmail,
                                                        String message,
                                                        Long relatedTenderId,
                                                        Long relatedBidId,
                                                        Long relatedTicketId) {
        NotificationOutbox notification = NotificationOutbox.builder()
                .notificationReference(generateNotificationReference())
                .channel(channel)
                .templateType(templateType)
                .status(NotificationOutbox.NotificationStatus.PENDING)
                .recipientUserId(recipientUserId)
                .recipientPhone(recipientPhone)
                .recipientEmail(recipientEmail)
                .message(message)
                .relatedTenderId(relatedTenderId)
                .relatedBidId(relatedBidId)
                .relatedTicketId(relatedTicketId)
                .retryCount(0)
                .build();

        NotificationOutbox saved = repository.saveAndFlush(notification);

        log.info(
                "Notification queued: id={} reference={} templateType={} recipientUserId={}",
                saved.getId(),
                saved.getNotificationReference(),
                saved.getTemplateType(),
                saved.getRecipientUserId()
        );

        return NotificationOutboxResponse.from(saved);
    }

    @Override
    public Page<NotificationOutboxResponse> getNotifications(Pageable pageable) {
        return repository.findAll(pageable)
                .map(NotificationOutboxResponse::from);
    }

    @Override
    public Page<NotificationOutboxResponse> getNotificationsByStatus(NotificationOutbox.NotificationStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable)
                .map(NotificationOutboxResponse::from);
    }

    @Override
    public NotificationOutboxResponse getNotificationById(Long id) {
        return NotificationOutboxResponse.from(findNotification(id));
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markSent(Long id) {
        return markSent(id, "MANUAL");
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markFailed(Long id, MarkNotificationFailedRequest request) {
        return markDeliveryFailed(
                id,
                request.getFailureReason().trim()
        );
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markSent(Long id, String providerMessageId) {
        NotificationOutbox notification = findNotification(id);

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.SENT) {

            throw new NotificationOutboxException("Notification has already been marked as sent.");
        }

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.CANCELLED) {
            throw new NotificationOutboxException("Cancelled notification cannot be marked as sent.");
        }

        LocalDateTime now = LocalDateTime.now();

        notification.setStatus(NotificationOutbox.NotificationStatus.SENT);
        notification.setProviderMessageId(providerMessageId);
        notification.setSentAt(now);
        notification.setUpdatedAt(now);
        notification.setFailureReason(null);

        NotificationOutbox saved = repository.save(notification);

        log.info(
                "Notification marked as sent: id={} providerMessageId={}",
                saved.getId(),
                saved.getProviderMessageId()
        );

        return NotificationOutboxResponse.from(saved);
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markDeliveryFailed(Long id, String failureReason) {
        NotificationOutbox notification = findNotification(id);

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.SENT) {

            throw new NotificationOutboxException("Sent notification cannot be marked as failed.");
        }

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.CANCELLED) {
            throw new NotificationOutboxException("Cancelled notification cannot be marked as failed.");
        }

        LocalDateTime now = LocalDateTime.now();

        notification.setStatus(NotificationOutbox.NotificationStatus.FAILED);
        notification.setFailureReason(failureReason);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setProviderMessageId(null);
        notification.setUpdatedAt(now);

        NotificationOutbox saved = repository.save(notification);

        log.warn(
                "Notification marked as failed: id={} retryCount={} reason={}",
                saved.getId(),
                saved.getRetryCount(),
                saved.getFailureReason()
        );

        return NotificationOutboxResponse.from(saved);
    }


    private NotificationOutbox findNotification(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotificationOutboxException(
                        "Notification not found with ID: " + id
                ));
    }

    private String generateNotificationReference() {
        return "KB-NOTIF-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
}
