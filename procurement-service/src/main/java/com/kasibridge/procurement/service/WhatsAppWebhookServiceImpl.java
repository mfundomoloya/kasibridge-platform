package com.kasibridge.procurement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookServiceImpl
        implements WhatsAppWebhookService {

    private final NotificationOutboxRepository repository;

    @Override
    @Transactional
    public void processWebhook(JsonNode payload) {

        if (payload == null || payload.isNull()) {
            log.warn(
                    "Ignoring empty WhatsApp webhook payload."
            );
            return;
        }

        JsonNode entries = payload.path("entry");

        if (!entries.isArray()) {
            log.info("Ignoring WhatsApp webhook without an entry array.");
            return;
        }

        for (JsonNode entry : entries) {
            processEntry(entry);
        }
    }

    private void processEntry(JsonNode entry) {
        JsonNode changes = entry.path("changes");

        if (!changes.isArray()) {
            return;
        }

        for (JsonNode change : changes) {
            JsonNode statuses =
                    change.path("value").path("statuses");

            if (!statuses.isArray()) {
                continue;
            }

            for (JsonNode statusNode : statuses) {
                processStatus(statusNode);
            }
        }
    }

    private void processStatus(JsonNode statusNode) {
        String providerMessageId =
                textOrNull(statusNode, "id");

        String providerStatus =
                textOrNull(statusNode, "status");

        if (providerMessageId == null
                || providerStatus == null) {
            log.warn(
                    "Ignoring WhatsApp status without message ID or status."
            );
            return;
        }

        providerMessageId =
                providerMessageId.trim();

        providerStatus =
                providerStatus.trim();

        NotificationOutbox notification =
                repository.findByProviderMessageId(
                                providerMessageId
                        )
                        .orElse(null);

        if (notification == null) {
            log.warn(
                    "Ignoring WhatsApp status for unknown providerMessageId={}",
                    providerMessageId
            );
            return;
        }

        NotificationOutbox.DeliveryStatus newStatus =
                mapStatus(providerStatus);

        if (newStatus == null) {
            log.info(
                    "Ignoring unsupported WhatsApp delivery status={} notificationId={}",
                    providerStatus,
                    notification.getId()
            );
            return;
        }

        LocalDateTime providerTimestamp =
                parseTimestamp(
                        textOrNull(statusNode, "timestamp")
                );

        if (isStaleOrDuplicate(
                notification.getDeliveryStatus(),
                newStatus
        )) {
            log.info(
                    "Ignoring stale or duplicate WhatsApp status: notificationId={} currentStatus={} incomingStatus={}",
                    notification.getId(),
                    notification.getDeliveryStatus(),
                    newStatus
            );
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveTimestamp = providerTimestamp != null
                ? providerTimestamp
                : now;

        notification.setDeliveryStatus(newStatus);
        notification.setProviderStatusTimestamp(effectiveTimestamp);
        notification.setUpdatedAt(now);

        if (newStatus == NotificationOutbox.DeliveryStatus.DELIVERED) {
            notification.setDeliveredAt(effectiveTimestamp);
        }

        if (newStatus == NotificationOutbox.DeliveryStatus.READ) {
            notification.setProviderReadAt(effectiveTimestamp);

            if (notification.getDeliveredAt() == null) {
                notification.setDeliveredAt(effectiveTimestamp);
            }
        }

        if (newStatus == NotificationOutbox.DeliveryStatus.FAILED) {
            notification.setProviderFailureCode(
                    extractFailureCode(statusNode)
            );

            notification.setProviderFailureReason(extractFailureReason(statusNode));
        }

      NotificationOutbox saved = repository.saveAndFlush(notification);

        log.info(
                "WhatsApp delivery status updated: notificationId={} deliveryStatus={}",
                saved.getId(),
                newStatus
        );
    }

    private NotificationOutbox.DeliveryStatus mapStatus(String status) {
        return switch (
                status.toLowerCase(Locale.ROOT)
                ) {
            case "sent" ->
                    NotificationOutbox.DeliveryStatus.SENT;
            case "delivered" ->
                    NotificationOutbox.DeliveryStatus.DELIVERED;
            case "read" ->
                    NotificationOutbox.DeliveryStatus.READ;
            case "failed" ->
                    NotificationOutbox.DeliveryStatus.FAILED;
            default -> null;
        };
    }

    private boolean isStaleOrDuplicate(NotificationOutbox.DeliveryStatus current,
                                       NotificationOutbox.DeliveryStatus incoming) {

        if (current == null) {
            return false;
        }

        if(current == incoming) {
            return true;
        }

        if(incoming == NotificationOutbox.DeliveryStatus.FAILED) {
            return current == NotificationOutbox.DeliveryStatus.READ;
        }

        if (current == NotificationOutbox.DeliveryStatus.FAILED) {
            return true;
        }

        return deliveryRank(incoming) <= deliveryRank(current);
    }

    private int deliveryRank(NotificationOutbox.DeliveryStatus status) {
        return switch (status) {
            case ACCEPTED -> 0;
            case SENT -> 1;
            case DELIVERED -> 2;
            case READ -> 3;
            case FAILED -> -1;
        };
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(
                            Long.parseLong(value)
                    ),
                    ZoneOffset.UTC
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "Unable to parse WhatsApp provider timestamp: value={}",
                    value
            );
            return null;
        }
    }

    private String extractFailureCode(JsonNode statusNode) {
        JsonNode errors = statusNode.path("errors");

        if (!errors.isArray() || errors.isEmpty()) {
            return null;
        }

        return textOrNull(errors.get(0), "code");
    }

    private String extractFailureReason(JsonNode statusNode) {
        JsonNode errors = statusNode.path("errors");

        if (!errors.isArray() || errors.isEmpty()) {
            return "WhatsApp delivery failed.";
        }

        JsonNode error = errors.get(0);

        String title = textOrNull(error, "title");

        String details = textOrNull(
                error.path("error_data"),
                "details"
        );

        String reason =
                details != null
                        ? details
                        : title;

        if (reason == null || reason.isBlank()) {
            return "WhatsApp delivery failed.";
        }

        String sanitized = reason
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();

        return sanitized.length() > 1000
                ? sanitized.substring(0, 1000)
                : sanitized;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);

        if (field.isMissingNode()
                || field.isNull()
                || field.asText().isBlank()) {
            return null;
        }

        return field.asText();
    }
}