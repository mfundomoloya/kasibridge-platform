package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {

    private final NotificationOutboxRepository repository;
    private final NotificationOutboxService outboxService;
    private final WhatsAppProvider whatsAppProvider;

    @Value("${kasibridge.notifications.dispatch.batch-size:20}")
    private int batchSize;

    @Value("${kasibridge.notifications.max-retries:3}")
    private int maxRetries;

    @Override
    public NotificationDispatchResponse dispatchPending() {

        List<NotificationOutbox> pendingNotifications = repository.findByStatusAndChannelOrderByCreatedAtAsc(
                NotificationOutbox.NotificationStatus.PENDING,
                NotificationOutbox.NotificationChannel.WHATSAPP,
                PageRequest.of(0, batchSize)
        );

        return dispatchBatch(pendingNotifications);
    }

    @Override
    public NotificationDispatchResponse retryFailed() {
        List<NotificationOutbox> retryableNotifications =
        repository.findByStatusInAndChannelAndRetryCountLessThanOrderByCreatedAtAsc(
                Set.of(NotificationOutbox.NotificationStatus.FAILED),
                NotificationOutbox.NotificationChannel.WHATSAPP,
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        return dispatchBatch(retryableNotifications);
    }

    @Override
    public NotificationOutboxResponse dispatchOne(Long notificationId) {

        NotificationOutbox notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotificationOutboxException(
                        "Notification not found with ID: " + notificationId
                ));

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.SENT) {
            throw new NotificationOutboxException("Notification has already been sent.");
        }

        if (notification.getStatus() == NotificationOutbox.NotificationStatus.CANCELLED) {
            throw new NotificationOutboxException("Cancelled notification cannot be dispatched.");
        }

        if (notification.getRetryCount() >= maxRetries) {
            throw new NotificationOutboxException("Notification has reached the maximum retry limit.");
        }

        dispatchNotification(notification);

        return outboxService.getNotificationById(notificationId);
    }

    private NotificationDispatchResponse dispatchBatch(List<NotificationOutbox> notifications
    ) {

        List<Long> sentIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        List<Long> skippedIds = new ArrayList<>();

        for (NotificationOutbox notification : notifications) {
            if (notification.getStatus() == NotificationOutbox.NotificationStatus.SENT
                    || notification.getStatus() == NotificationOutbox.NotificationStatus.CANCELLED
                    || notification.getRetryCount() >= maxRetries) {

                skippedIds.add(notification.getId());
                continue;
            }

            try {
                boolean sent = dispatchNotification(notification);

                if (sent) {
                    sentIds.add(notification.getId());
                } else {
                    failedIds.add(notification.getId());
                }

            }  catch (Exception exception) {
            log.error(
                    "Unexpected dispatch failure for notificationId={}",
                    notification.getId(),
                    exception
            );

            try {
                markDispatchFailed(
                        notification.getId(),
                        "Unexpected dispatcher error: "
                                + safeMessage(exception)
                );
            } catch (Exception markingException) {
                log.error(
                        "Failed to record dispatch failure "
                                + "for notificationId={}",
                        notification.getId(),
                        markingException
                );
            }

            failedIds.add(notification.getId());
        }
        }

        return NotificationDispatchResponse.builder()
                .selected(notifications.size())
                .processed(sentIds.size() + failedIds.size())
                .sent(sentIds.size())
                .failed(failedIds.size())
                .skipped(skippedIds.size())
                .sentNotificationIds(sentIds)
                .failedNotificationIds(failedIds)
                .skippedNotificationIds(skippedIds)
                .build();
    }

    private boolean dispatchNotification(
            NotificationOutbox notification
    ) {
        if (notification.getChannel()
                != NotificationOutbox.NotificationChannel.WHATSAPP) {
            markDispatchFailed(
                    notification.getId(),
                    "No provider is configured for channel: "
                            + notification.getChannel()
            );

            return false;
        }

        if (notification.getRecipientPhone() == null
                || notification.getRecipientPhone().isBlank()) {
            markDispatchFailed(
                    notification.getId(),
                    "WhatsApp recipient phone number is missing."
            );

            return false;
        }

        if (notification.getMessage() == null
                || notification.getMessage().isBlank()) {
            markDispatchFailed(
                    notification.getId(),
                    "Notification message is missing."
            );

            return false;
        }

        WhatsAppDeliveryResult result =
                whatsAppProvider.sendMessage(
                        notification.getRecipientPhone(),
                        notification.getMessage()
                );

        if (result == null) {
            markDispatchFailed(
                    notification.getId(),
                    "WhatsApp provider returned no delivery result."
            );

            return false;
        }

        if (result.successful()) {
            if (result.providerMessageId() == null
                    || result.providerMessageId().isBlank()) {
                markDispatchFailed(
                        notification.getId(),
                        "WhatsApp provider returned a successful result "
                                + "without a provider message ID."
                );

                return false;
            }

            outboxService.markSent(
                    notification.getId(),
                    result.providerMessageId()
            );

            return true;
        }

        String failureReason =
                result.failureReason() == null
                        || result.failureReason().isBlank()
                        ? "WhatsApp provider rejected the message."
                        : result.failureReason();

        markDispatchFailed(
                notification.getId(),
                failureReason
        );

        return false;
    }
    private void markDispatchFailed(
            Long notificationId,
            String failureReason
    ) {
        MarkNotificationFailedRequest request = new MarkNotificationFailedRequest();

        request.setFailureReason(failureReason);

        outboxService.markFailed(notificationId, request);
    }

    private String safeMessage(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {

            return ex.getClass().getSimpleName();
        }

        String message = ex.getMessage();

        return message.length() > 900
                ? message.substring(0, 900)
: message;
    }
}
