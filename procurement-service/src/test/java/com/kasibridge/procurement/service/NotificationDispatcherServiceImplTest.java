package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceImplTest {

    @Mock
    private NotificationOutboxRepository repository;

    @Mock
    private NotificationOutboxService outboxService;

    @Mock
    private WhatsAppProvider whatsAppProvider;

    @InjectMocks
    private NotificationDispatcherServiceImpl dispatcherService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                dispatcherService,
                "batchSize",
                20
        );

        ReflectionTestUtils.setField(
                dispatcherService,
                "maxRetries",
                3
        );
    }

    @Test
    void dispatchPending_shouldSendPendingWhatsAppNotification() {
        NotificationOutbox notification =
                createNotification(
                        1L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        "+27610000001",
                        "Your support ticket was created."
                );

        when(repository
                .findByStatusAndChannelOrderByCreatedAtAsc(
                        eq(NotificationOutbox.NotificationStatus.PENDING),
                        eq(NotificationOutbox.NotificationChannel.WHATSAPP),
                        any(PageRequest.class)
                ))
                .thenReturn(List.of(notification));

        when(whatsAppProvider.sendMessage(
                "+27610000001",
                "Your support ticket was created."
        )).thenReturn(
                WhatsAppDeliveryResult.success(
                        "wamid.TEST001"
                )
        );

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        assertNotNull(response);

        verify(whatsAppProvider).sendMessage(
                "+27610000001",
                "Your support ticket was created."
        );

        verify(outboxService).markSent(
                1L,
                "wamid.TEST001"
        );

        verify(outboxService, never()).markFailed(
                eq(1L),
                any(MarkNotificationFailedRequest.class)
        );
    }

    @Test
    void retryFailed_shouldDispatchEligibleFailedNotification() {
        NotificationOutbox notification =
                createNotification(
                        2L,
                        NotificationOutbox.NotificationStatus.FAILED,
                        1,
                        "+27610000002",
                        "Retry notification."
                );

        when(repository
                .findByStatusInAndChannelAndRetryCountLessThanOrderByCreatedAtAsc(
                        eq(Set.of(
                                NotificationOutbox.NotificationStatus.FAILED
                        )),
                        eq(NotificationOutbox.NotificationChannel.WHATSAPP),
                        eq(3),
                        any(PageRequest.class)
                ))
                .thenReturn(List.of(notification));

        when(whatsAppProvider.sendMessage(
                "+27610000002",
                "Retry notification."
        )).thenReturn(
                WhatsAppDeliveryResult.success(
                        "wamid.RETRY001"
                )
        );

        NotificationDispatchResponse response =
                dispatcherService.retryFailed();

        assertNotNull(response);

        verify(whatsAppProvider).sendMessage(
                "+27610000002",
                "Retry notification."
        );

        verify(outboxService).markSent(
                2L,
                "wamid.RETRY001"
        );
    }

    @Test
    void dispatchPending_shouldMarkProviderRejectionAsFailed() {
        NotificationOutbox notification =
                createNotification(
                        3L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        "+27610000003",
                        "Provider rejection test."
                );

        when(repository
                .findByStatusAndChannelOrderByCreatedAtAsc(
                        eq(NotificationOutbox.NotificationStatus.PENDING),
                        eq(NotificationOutbox.NotificationChannel.WHATSAPP),
                        any(PageRequest.class)
                ))
                .thenReturn(List.of(notification));

        when(whatsAppProvider.sendMessage(
                "+27610000003",
                "Provider rejection test."
        )).thenReturn(
                WhatsAppDeliveryResult.failure(
                        "WhatsApp provider rejected the message."
                )
        );

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        assertNotNull(response);

        ArgumentCaptor<MarkNotificationFailedRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        MarkNotificationFailedRequest.class
                );

        verify(outboxService).markFailed(
                eq(3L),
                requestCaptor.capture()
        );

        assertEquals(
                "WhatsApp provider rejected the message.",
                requestCaptor.getValue().getFailureReason()
        );

        verify(outboxService, never()).markSent(
                eq(3L),
                any(String.class)
        );
    }

    @Test
    void dispatchPending_shouldMarkMissingPhoneNumberAsFailed() {
        NotificationOutbox notification =
                createNotification(
                        4L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        null,
                        "Notification without a phone number."
                );

        when(repository
                .findByStatusAndChannelOrderByCreatedAtAsc(
                        eq(NotificationOutbox.NotificationStatus.PENDING),
                        eq(NotificationOutbox.NotificationChannel.WHATSAPP),
                        any(PageRequest.class)
                ))
                .thenReturn(List.of(notification));

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        assertNotNull(response);

        ArgumentCaptor<MarkNotificationFailedRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        MarkNotificationFailedRequest.class
                );

        verify(outboxService).markFailed(
                eq(4L),
                requestCaptor.capture()
        );

        assertEquals(
                "WhatsApp recipient phone number is missing.",
                requestCaptor.getValue().getFailureReason()
        );

        verify(whatsAppProvider, never()).sendMessage(
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void dispatchPending_shouldMarkMissingMessageAsFailed() {
        NotificationOutbox notification =
                createNotification(
                        5L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        "+27610000005",
                        null
                );

        when(repository
                .findByStatusAndChannelOrderByCreatedAtAsc(
                        eq(NotificationOutbox.NotificationStatus.PENDING),
                        eq(NotificationOutbox.NotificationChannel.WHATSAPP),
                        any(PageRequest.class)
                ))
                .thenReturn(List.of(notification));

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        assertNotNull(response);

        ArgumentCaptor<MarkNotificationFailedRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        MarkNotificationFailedRequest.class
                );

        verify(outboxService).markFailed(
                eq(5L),
                requestCaptor.capture()
        );

        assertEquals(
                "Notification message is missing.",
                requestCaptor.getValue().getFailureReason()
        );

        verify(whatsAppProvider, never()).sendMessage(
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void dispatchOne_shouldRejectAlreadySentNotification() {
        NotificationOutbox notification =
                createNotification(
                        6L,
                        NotificationOutbox.NotificationStatus.SENT,
                        0,
                        "+27610000006",
                        "Already sent."
                );

        when(repository.findById(6L))
                .thenReturn(Optional.of(notification));

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> dispatcherService.dispatchOne(6L)
                );

        assertEquals(
                "Notification has already been sent.",
                exception.getMessage()
        );

        verify(whatsAppProvider, never()).sendMessage(
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void dispatchOne_shouldRejectCancelledNotification() {
        NotificationOutbox notification =
                createNotification(
                        7L,
                        NotificationOutbox.NotificationStatus.CANCELLED,
                        0,
                        "+27610000007",
                        "Cancelled notification."
                );

        when(repository.findById(7L))
                .thenReturn(Optional.of(notification));

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> dispatcherService.dispatchOne(7L)
                );

        assertEquals(
                "Cancelled notification cannot be dispatched.",
                exception.getMessage()
        );

        verify(whatsAppProvider, never()).sendMessage(
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void dispatchOne_shouldRejectNotificationAtMaximumRetries() {
        NotificationOutbox notification =
                createNotification(
                        8L,
                        NotificationOutbox.NotificationStatus.FAILED,
                        3,
                        "+27610000008",
                        "Maximum retry notification."
                );

        when(repository.findById(8L))
                .thenReturn(Optional.of(notification));

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> dispatcherService.dispatchOne(8L)
                );

        assertEquals(
                "Notification has reached the maximum retry limit.",
                exception.getMessage()
        );

        verify(whatsAppProvider, never()).sendMessage(
                any(String.class),
                any(String.class)
        );
    }

    @Test
    void dispatchOne_shouldReturnUpdatedNotificationAfterSuccess() {
        NotificationOutbox notification =
                createNotification(
                        9L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        "+27610000009",
                        "Single dispatch notification."
                );

        NotificationOutboxResponse expectedResponse =
                org.mockito.Mockito.mock(
                        NotificationOutboxResponse.class
                );

        when(repository.findById(9L))
                .thenReturn(Optional.of(notification));

        when(whatsAppProvider.sendMessage(
                "+27610000009",
                "Single dispatch notification."
        )).thenReturn(
                WhatsAppDeliveryResult.success(
                        "wamid.SINGLE001"
                )
        );

        when(outboxService.getNotificationById(9L))
                .thenReturn(expectedResponse);

        NotificationOutboxResponse actualResponse =
                dispatcherService.dispatchOne(9L);

        assertEquals(
                expectedResponse,
                actualResponse
        );

        verify(outboxService).markSent(
                9L,
                "wamid.SINGLE001"
        );

        verify(outboxService).getNotificationById(9L);
    }

    @Test
    void dispatchPending_shouldContinueAfterProviderException() {
        NotificationOutbox firstNotification =
                createNotification(
                        10L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        "+27610000010",
                        "First notification."
                );

        NotificationOutbox secondNotification =
                createNotification(
                        11L,
                        NotificationOutbox.NotificationStatus.PENDING,
                        0,
                        "+27610000011",
                        "Second notification."
                );

        when(repository
                .findByStatusAndChannelOrderByCreatedAtAsc(
                        eq(NotificationOutbox.NotificationStatus.PENDING),
                        eq(NotificationOutbox.NotificationChannel.WHATSAPP),
                        any(PageRequest.class)
                ))
                .thenReturn(
                        List.of(
                                firstNotification,
                                secondNotification
                        )
                );

        when(whatsAppProvider.sendMessage(
                "+27610000010",
                "First notification."
        )).thenThrow(
                new IllegalStateException(
                        "Provider connection failed."
                )
        );

        when(whatsAppProvider.sendMessage(
                "+27610000011",
                "Second notification."
        )).thenReturn(
                WhatsAppDeliveryResult.success(
                        "wamid.SECOND001"
                )
        );

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        assertNotNull(response);

        ArgumentCaptor<MarkNotificationFailedRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        MarkNotificationFailedRequest.class
                );

        verify(outboxService).markFailed(
                eq(10L),
                requestCaptor.capture()
        );

        assertTrue(
                requestCaptor.getValue()
                        .getFailureReason()
                        .contains("Provider connection failed")
        );

        verify(outboxService).markSent(
                11L,
                "wamid.SECOND001"
        );
    }

    private NotificationOutbox createNotification(
            Long id,
            NotificationOutbox.NotificationStatus status,
            int retryCount,
            String recipientPhone,
            String message
    ) {
        return NotificationOutbox.builder()
                .id(id)
                .notificationReference(
                        "KB-NOTIF-" + id
                )
                .channel(
                        NotificationOutbox.NotificationChannel.WHATSAPP
                )
                .templateType(
                        NotificationOutbox.NotificationTemplateType
                                .SUPPORT_TICKET_CREATED
                )
                .status(status)
                .recipientUserId(5L)
                .recipientPhone(recipientPhone)
                .message(message)
                .retryCount(retryCount)
                .build();
    }
}