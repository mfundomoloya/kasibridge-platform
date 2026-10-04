package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.exception.NotificationStateException;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
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
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public NotificationOutboxResponse queueNotification(NotificationOutbox.NotificationChannel channel,
                                                        NotificationOutbox.NotificationTemplateType templateType,
                                                        Long recipientUserId,
                                                        String recipientPhone,
                                                        String recipientEmail,
                                                        String message,
                                                        Long relatedTenderId,
                                                        Long relatedBidId,
                                                        Long relatedTicketId) {
        validateQueueRequest(
                channel,
                templateType,
                recipientUserId,
                recipientPhone,
                recipientEmail,
                message
        );

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
    @Transactional(readOnly = true)
    public Page<NotificationOutboxResponse> getNotifications(Pageable pageable) {
        return repository.findAll(pageable)
                .map(NotificationOutboxResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationOutboxResponse> getNotificationsByStatus(NotificationOutbox.NotificationStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable)
                .map(NotificationOutboxResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationOutboxResponse getNotificationById(Long id) {
        return NotificationOutboxResponse.from(findNotification(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationOutboxResponse> getInAppNotifications(Pageable pageable) {

        if(currentUserService.hasRole("ROLE_PLATFORM_ADMIN")){
            return repository.findByChannelAndReadAtIsNotNull(NotificationOutbox.NotificationChannel.IN_APP, pageable)
                    .map(NotificationOutboxResponse::from);
        }

        Long currentUserId = currentUserService.getCurrentUserId();

        return repository.findByChannelAndRecipientUserIdAndReadAtIsNotNull(
                NotificationOutbox.NotificationChannel.IN_APP,
                        currentUserId,
                        pageable
                )
                .map(NotificationOutboxResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationOutboxResponse> getUnreadInAppNotifications(Pageable pageable) {

        boolean isPlatformAdmin = currentUserService.hasRole("ROLE_PLATFORM_ADMIN");

        if (isPlatformAdmin) {
            return repository.findByChannelAndReadAtIsNull(
                    NotificationOutbox.NotificationChannel.IN_APP,
                    pageable
            )
                    .map(NotificationOutboxResponse::from);
        }

        Long currentUserId = currentUserService.getCurrentUserId();

        log.info("Loading unread in-app notifications for scoped userId={}", currentUserId);

        return repository.findByChannelAndRecipientUserIdAndReadAtIsNull(
                NotificationOutbox.NotificationChannel.IN_APP,
                        currentUserId,
                        pageable
                )
                .map(NotificationOutboxResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationOutboxResponse> getReadInAppNotifications(Pageable pageable) {

        if (currentUserService.hasRole("ROLE_PLATFORM_ADMIN")) {
            return repository.findByChannelAndReadAtIsNull(
                            NotificationOutbox.NotificationChannel.IN_APP, pageable)
                    .map(NotificationOutboxResponse::from);
        }

        Long currentUserId = currentUserService.getCurrentUserId();

        return repository.findByChannelAndRecipientUserIdAndReadAtIsNotNull(
                NotificationOutbox.NotificationChannel.IN_APP,
                currentUserId,
                pageable
                )
                .map(NotificationOutboxResponse::from);
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markInAppNotificationRead(Long notificationId) {
        Long actorUserId = currentUserService.getCurrentUserId();

        NotificationOutbox notification = findNotification(notificationId);

        if (notification.getChannel()
                != NotificationOutbox.NotificationChannel.IN_APP) {
            throw new NotificationOutboxException("Only IN_APP notifications can be marked as read.");
        }

        boolean platformAdmin = currentUserService.hasRole("ROLE_PLATFORM_ADMIN");

        boolean intendedRecipient = notification.getRecipientUserId() != null
                && notification.getRecipientUserId().equals(actorUserId);

        if(!platformAdmin && !intendedRecipient){
            throw new ProcurementAuthorizationException("User is not authorized to acknowledge this notification.");
        }

        if (notification.getReadAt() != null) {
            throw new NotificationStateException("In-app notification has already been marked as read.");
        }

        LocalDateTime now = LocalDateTime.now();

        notification.setReadByUserId(actorUserId);
        notification.setReadAt(now);
        notification.setUpdatedAt(now);

        NotificationOutbox saved = repository.saveAndFlush(notification);

        log.info("In-app notification marked as read: notificationId={} readByUserId={}", saved.getId(), actorUserId);

        return NotificationOutboxResponse.from(saved);
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markSent(Long id) {
        return markSent(id, "MANUAL");
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markFailed(Long id, MarkNotificationFailedRequest request) {

        if (request == null
                || request.getFailureReason() == null
                || request.getFailureReason().isBlank()) {
            throw new NotificationOutboxException(
                    "A notification failure reason is required."
            );
        }

        NotificationOutbox notification =
                findNotification(id);

        if (notification.getStatus()
                == NotificationOutbox.NotificationStatus.SENT) {
            throw new NotificationStateException(
                    "A sent notification cannot be marked "
                            + "as a sending failure."
            );
        }

        if (notification.getStatus()
                == NotificationOutbox.NotificationStatus.CANCELLED) {
            throw new NotificationStateException(
                    "A cancelled notification cannot be marked as failed."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        notification.setStatus(
                NotificationOutbox.NotificationStatus.FAILED
        );
        notification.setFailureReason(
                request.getFailureReason().trim()
        );
        notification.setRetryCount(
                notification.getRetryCount() + 1
        );
        notification.setProviderMessageId(null);
        notification.setUpdatedAt(now);

        NotificationOutbox saved =
                repository.saveAndFlush(notification);

        return NotificationOutboxResponse.from(saved);
    }

    @Override
    @Transactional
    public NotificationOutboxResponse markSent(Long id, String providerMessageId) {

        if (providerMessageId == null || providerMessageId.isBlank()) {
            throw new NotificationOutboxException("A provider message ID is required.");
        }

        NotificationOutbox notification = findNotification(id);

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.SENT) {

            throw new NotificationOutboxException("Notification has already been marked as sent.");
        }

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.CANCELLED) {
            throw new NotificationOutboxException("Cancelled notification cannot be marked as sent.");
        }

        LocalDateTime now = LocalDateTime.now();

        notification.setStatus(NotificationOutbox.NotificationStatus.SENT);
        notification.setProviderMessageId(providerMessageId.trim());
        notification.setDeliveryStatus(NotificationOutbox.DeliveryStatus.ACCEPTED);
        notification.setProviderStatusTimestamp(now);
        notification.setProviderFailureCode(null);
        notification.setProviderFailureReason(null);
        notification.setSentAt(now);
        notification.setUpdatedAt(now);
        notification.setFailureReason(null);

        NotificationOutbox saved = repository.saveAndFlush(notification);

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

        if (failureReason == null || failureReason.isBlank()) {
            throw new NotificationOutboxException("A delivery failure reason is required.");
        }

        NotificationOutbox notification = findNotification(id);

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.SENT) {

            throw new NotificationOutboxException("Sent notification cannot be marked as failed.");
        }

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.CANCELLED) {
            throw new NotificationOutboxException("Cancelled notification cannot be marked as failed.");
        }

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.FAILED) {
            throw new NotificationOutboxException("Notification has already been marked as failed.");
        }

        LocalDateTime now = LocalDateTime.now();

        notification.setDeliveryStatus(NotificationOutbox.DeliveryStatus.FAILED);
        notification.setProviderFailureReason(failureReason.trim());
        notification.setProviderStatusTimestamp(now);
        notification.setUpdatedAt(now);

        NotificationOutbox saved = repository.saveAndFlush(notification);

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

    private void validateQueueRequest(
            NotificationOutbox.NotificationChannel channel,
            NotificationOutbox.NotificationTemplateType templateType,
            Long recipientUserId,
            String recipientPhone,
            String recipientEmail,
            String message
    ) {
        if (channel == null) {
            throw new NotificationOutboxException(
                    "Notification channel is required."
            );
        }

        if (templateType == null) {
            throw new NotificationOutboxException(
                    "Notification template type is required."
            );
        }

        if (message == null || message.isBlank()) {
            throw new NotificationOutboxException(
                    "Notification message is required."
            );
        }

        if (channel
                == NotificationOutbox.NotificationChannel.WHATSAPP
                && (recipientPhone == null
                || recipientPhone.isBlank())) {
            throw new NotificationOutboxException(
                    "A recipient phone number is required "
                            + "for WhatsApp notifications."
            );
        }

        if (channel
                == NotificationOutbox.NotificationChannel.EMAIL
                && (recipientEmail == null
                || recipientEmail.isBlank())) {
            throw new NotificationOutboxException(
                    "A recipient email address is required "
                            + "for email notifications."
            );
        }

        if (channel
                == NotificationOutbox.NotificationChannel.IN_APP
                && recipientUserId == null) {
            throw new NotificationOutboxException(
                    "A recipient user ID is required "
                            + "for in-app notifications."
            );
        }
    }
}
