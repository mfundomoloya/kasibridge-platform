package com.kasibridge.procurement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookServiceImplTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Mock
    private NotificationOutboxRepository repository;

    @InjectMocks
    private WhatsAppWebhookServiceImpl webhookService;

    @Test
    void processWebhook_shouldSetDeliveryStatusToSent()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.ACCEPTED
                );

        when(repository.findByProviderMessageId(
                "wamid.TEST001"
        )).thenReturn(Optional.of(notification));

        when(repository.saveAndFlush(
                any(NotificationOutbox.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.TEST001",
                        "sent",
                        "1791062400"
                )
        );

        webhookService.processWebhook(payload);

        assertEquals(
                NotificationOutbox.DeliveryStatus.SENT,
                notification.getDeliveryStatus()
        );

        assertNotNull(
                notification.getProviderStatusTimestamp()
        );

        verify(repository).saveAndFlush(notification);
    }

    @Test
    void processWebhook_shouldSetDeliveredTimestamp()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.SENT
                );

        when(repository.findByProviderMessageId(
                "wamid.TEST002"
        )).thenReturn(Optional.of(notification));

        when(repository.saveAndFlush(
                any(NotificationOutbox.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.TEST002",
                        "delivered",
                        "1791062460"
                )
        );

        webhookService.processWebhook(payload);

        assertEquals(
                NotificationOutbox.DeliveryStatus.DELIVERED,
                notification.getDeliveryStatus()
        );

        assertNotNull(notification.getDeliveredAt());

        verify(repository).saveAndFlush(notification);
    }

    @Test
    void processWebhook_shouldSetReadAndImpliedDeliveredTimestamp()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.SENT
                );

        when(repository.findByProviderMessageId(
                "wamid.TEST003"
        )).thenReturn(Optional.of(notification));

        when(repository.saveAndFlush(
                any(NotificationOutbox.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.TEST003",
                        "read",
                        "1791062520"
                )
        );

        webhookService.processWebhook(payload);

        assertEquals(
                NotificationOutbox.DeliveryStatus.READ,
                notification.getDeliveryStatus()
        );

        assertNotNull(
                notification.getProviderReadAt()
        );

        assertNotNull(
                notification.getDeliveredAt()
        );

        verify(repository).saveAndFlush(notification);
    }

    @Test
    void processWebhook_shouldStoreProviderFailureDetails()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.SENT
                );

        when(repository.findByProviderMessageId(
                "wamid.TEST004"
        )).thenReturn(Optional.of(notification));

        when(repository.saveAndFlush(
                any(NotificationOutbox.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        String payloadJson = """
                {
                  "entry": [
                    {
                      "changes": [
                        {
                          "value": {
                            "statuses": [
                              {
                                "id": "wamid.TEST004",
                                "status": "failed",
                                "timestamp": "1791062580",
                                "errors": [
                                  {
                                    "code": 131026,
                                    "title": "Message undeliverable",
                                    "error_data": {
                                      "details": "The message could not be delivered."
                                    }
                                  }
                                ]
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        JsonNode payload =
                objectMapper.readTree(payloadJson);

        webhookService.processWebhook(payload);

        assertEquals(
                NotificationOutbox.DeliveryStatus.FAILED,
                notification.getDeliveryStatus()
        );

        assertEquals(
                "131026",
                notification.getProviderFailureCode()
        );

        assertEquals(
                "The message could not be delivered.",
                notification.getProviderFailureReason()
        );

        verify(repository).saveAndFlush(notification);
    }

    @Test
    void processWebhook_shouldIgnoreDuplicateStatus()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.DELIVERED
                );

        LocalDateTime originalTimestamp =
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        10,
                        30
                );

        notification.setProviderStatusTimestamp(
                originalTimestamp
        );

        when(repository.findByProviderMessageId(
                "wamid.TEST005"
        )).thenReturn(Optional.of(notification));

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.TEST005",
                        "delivered",
                        "1791062640"
                )
        );

        webhookService.processWebhook(payload);

        assertEquals(
                originalTimestamp,
                notification.getProviderStatusTimestamp()
        );

        verify(repository, never())
                .saveAndFlush(any(NotificationOutbox.class));
    }

    @Test
    void processWebhook_shouldIgnoreStaleSentStatus()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.DELIVERED
                );

        when(repository.findByProviderMessageId(
                "wamid.TEST006"
        )).thenReturn(Optional.of(notification));

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.TEST006",
                        "sent",
                        "1791062700"
                )
        );

        webhookService.processWebhook(payload);

        assertEquals(
                NotificationOutbox.DeliveryStatus.DELIVERED,
                notification.getDeliveryStatus()
        );

        verify(repository, never())
                .saveAndFlush(any(NotificationOutbox.class));
    }

    @Test
    void processWebhook_shouldIgnoreUnknownProviderMessageId()
            throws Exception {

        when(repository.findByProviderMessageId(
                "wamid.UNKNOWN"
        )).thenReturn(Optional.empty());

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.UNKNOWN",
                        "delivered",
                        "1791062760"
                )
        );

        webhookService.processWebhook(payload);

        verify(repository, never())
                .saveAndFlush(any(NotificationOutbox.class));
    }

    @Test
    void processWebhook_shouldUseCurrentTimeForMalformedTimestamp()
            throws Exception {

        NotificationOutbox notification =
                createSentNotification(
                        NotificationOutbox.DeliveryStatus.SENT
                );

        when(repository.findByProviderMessageId(
                "wamid.TEST007"
        )).thenReturn(Optional.of(notification));

        when(repository.saveAndFlush(
                any(NotificationOutbox.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        JsonNode payload = objectMapper.readTree(
                createStatusPayload(
                        "wamid.TEST007",
                        "delivered",
                        "invalid-timestamp"
                )
        );

        webhookService.processWebhook(payload);

        assertEquals(
                NotificationOutbox.DeliveryStatus.DELIVERED,
                notification.getDeliveryStatus()
        );

        assertNotNull(
                notification.getProviderStatusTimestamp()
        );

        assertNotNull(
                notification.getDeliveredAt()
        );

        verify(repository).saveAndFlush(notification);
    }

    @Test
    void processWebhook_shouldIgnorePayloadWithoutEntryArray()
            throws Exception {

        JsonNode payload =
                objectMapper.readTree(
                        """
                        {
                          "object": "whatsapp_business_account"
                        }
                        """
                );

        webhookService.processWebhook(payload);

        verify(repository, never())
                .findByProviderMessageId(any());

        verify(repository, never())
                .saveAndFlush(any(NotificationOutbox.class));
    }

    private NotificationOutbox createSentNotification(
            NotificationOutbox.DeliveryStatus deliveryStatus
    ) {
        return NotificationOutbox.builder()
                .id(1L)
                .notificationReference("KB-NOTIF-TEST0001")
                .channel(
                        NotificationOutbox.NotificationChannel.WHATSAPP
                )
                .templateType(
                        NotificationOutbox.NotificationTemplateType
                                .SUPPORT_TICKET_RESPONDED
                )
                .status(
                        NotificationOutbox.NotificationStatus.SENT
                )
                .recipientUserId(5L)
                .recipientPhone("+27610000001")
                .message("Test WhatsApp notification")
                .providerMessageId("wamid.TEST")
                .deliveryStatus(deliveryStatus)
                .retryCount(0)
                .build();
    }

    private String createStatusPayload(
            String providerMessageId,
            String status,
            String timestamp
    ) {
        return """
                {
                  "entry": [
                    {
                      "changes": [
                        {
                          "value": {
                            "statuses": [
                              {
                                "id": "%s",
                                "status": "%s",
                                "timestamp": "%s"
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                providerMessageId,
                status,
                timestamp
        );
    }
}