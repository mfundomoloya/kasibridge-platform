package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.exception.NotificationStateException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceImplTest {

    @Mock
    private NotificationOutboxRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NotificationOutboxServiceImpl outboxService;

    @Test
    void markFailed_shouldPreserveAttemptCountAndScheduleRetry() {
        LocalDateTime claimedAt =
                LocalDateTime.now()
                        .minusSeconds(10);

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .id(1L)
                        .notificationReference(
                                "KB-NOTIF-BACKOFF-01"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .PROCESSING
                        )
                        .recipientUserId(101L)
                        .recipientPhone(
                                "+27610000101"
                        )
                        .message(
                                "Retry backoff test."
                        )
                        .retryCount(1)
                        .claimedAt(claimedAt)
                        .claimedBy(
                                "dispatcher-worker"
                        )
                        .claimExpiresAt(
                                claimedAt.plusMinutes(5)
                        )
                        .createdAt(
                                claimedAt.minusMinutes(1)
                        )
                        .updatedAt(claimedAt)
                        .build();

        MarkNotificationFailedRequest request =
                new MarkNotificationFailedRequest();

        request.setFailureReason(
                " Temporary provider failure. "
        );

        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(notification)
                );

        when(repository.saveAndFlush(
                notification
        )).thenReturn(notification);

        LocalDateTime beforeFailure =
                LocalDateTime.now();

        outboxService.markFailed(
                1L,
                request
        );

        LocalDateTime afterFailure =
                LocalDateTime.now();

        ArgumentCaptor<NotificationOutbox> captor =
                ArgumentCaptor.forClass(
                        NotificationOutbox.class
                );

        verify(repository).saveAndFlush(
                captor.capture()
        );

        NotificationOutbox saved =
                captor.getValue();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .FAILED,
                saved.getStatus()
        );

        assertEquals(
                1,
                saved.getRetryCount()
        );

        assertEquals(
                "Temporary provider failure.",
                saved.getFailureReason()
        );

        assertNotNull(
                saved.getNextAttemptAt()
        );

        assertTrue(
                !saved.getNextAttemptAt()
                        .isBefore(
                                beforeFailure.plusSeconds(60)
                        )
        );

        assertTrue(
                !saved.getNextAttemptAt()
                        .isAfter(
                                afterFailure.plusSeconds(60)
                        )
        );

        assertNull(
                saved.getClaimedAt()
        );

        assertNull(
                saved.getClaimedBy()
        );

        assertNull(
                saved.getClaimExpiresAt()
        );
    }

    @Test
    void markFailed_shouldApplyExponentialBackoffForLaterAttempt() {
        LocalDateTime claimedAt =
                LocalDateTime.now()
                        .minusSeconds(10);

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .id(2L)
                        .notificationReference(
                                "KB-NOTIF-BACKOFF-02"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .PROCESSING
                        )
                        .recipientUserId(102L)
                        .recipientPhone(
                                "+27610000102"
                        )
                        .message(
                                "Second-attempt backoff test."
                        )
                        .retryCount(2)
                        .claimedAt(claimedAt)
                        .claimedBy(
                                "dispatcher-worker"
                        )
                        .claimExpiresAt(
                                claimedAt.plusMinutes(5)
                        )
                        .createdAt(
                                claimedAt.minusMinutes(1)
                        )
                        .updatedAt(claimedAt)
                        .build();

        MarkNotificationFailedRequest request =
                new MarkNotificationFailedRequest();

        request.setFailureReason(
                "Provider remains unavailable."
        );

        when(repository.findById(2L))
                .thenReturn(
                        Optional.of(notification)
                );

        when(repository.saveAndFlush(
                notification
        )).thenReturn(notification);

        LocalDateTime beforeFailure =
                LocalDateTime.now();

        outboxService.markFailed(
                2L,
                request
        );

        LocalDateTime afterFailure =
                LocalDateTime.now();

        ArgumentCaptor<NotificationOutbox> captor =
                ArgumentCaptor.forClass(
                        NotificationOutbox.class
                );

        verify(repository).saveAndFlush(
                captor.capture()
        );

        NotificationOutbox saved =
                captor.getValue();

        assertEquals(
                2,
                saved.getRetryCount()
        );

        assertNotNull(
                saved.getNextAttemptAt()
        );

        assertTrue(
                !saved.getNextAttemptAt()
                        .isBefore(
                                beforeFailure.plusSeconds(120)
                        )
        );

        assertTrue(
                !saved.getNextAttemptAt()
                        .isAfter(
                                afterFailure.plusSeconds(120)
                        )
        );

        assertNull(
                saved.getClaimedAt()
        );

        assertNull(
                saved.getClaimedBy()
        );

        assertNull(
                saved.getClaimExpiresAt()
        );
    }

    @Test
    void markFailed_shouldRejectBlankFailureReason() {
        MarkNotificationFailedRequest request =
                new MarkNotificationFailedRequest();

        request.setFailureReason("   ");

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> outboxService.markFailed(
                                3L,
                                request
                        )
                );

        assertEquals(
                "A notification failure reason is required.",
                exception.getMessage()
        );

        verify(repository, never())
                .findById(anyLong());

        verify(repository, never())
                .saveAndFlush(any());
    }

    @Test
    void markFailed_shouldRejectSentNotification() {
        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .id(4L)
                        .notificationReference(
                                "KB-NOTIF-BACKOFF-04"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .SENT
                        )
                        .recipientUserId(104L)
                        .recipientPhone(
                                "+27610000104"
                        )
                        .message(
                                "Already sent notification."
                        )
                        .retryCount(1)
                        .build();

        MarkNotificationFailedRequest request =
                new MarkNotificationFailedRequest();

        request.setFailureReason(
                "Late provider error."
        );

        when(repository.findById(4L))
                .thenReturn(
                        Optional.of(notification)
                );

        NotificationStateException exception =
                assertThrows(
                        NotificationStateException.class,
                        () -> outboxService.markFailed(
                                4L,
                                request
                        )
                );

        assertEquals(
                "A sent notification cannot be marked "
                        + "as a sending failure.",
                exception.getMessage()
        );

        verify(repository, never())
                .saveAndFlush(any());
    }


    @Test
    void markFailed_shouldRejectCancelledNotification() {
        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .id(5L)
                        .notificationReference(
                                "KB-NOTIF-BACKOFF-05"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .CANCELLED
                        )
                        .recipientUserId(105L)
                        .recipientPhone(
                                "+27610000105"
                        )
                        .message(
                                "Cancelled notification."
                        )
                        .retryCount(1)
                        .build();

        MarkNotificationFailedRequest request =
                new MarkNotificationFailedRequest();

        request.setFailureReason(
                "Provider failure."
        );

        when(repository.findById(5L))
                .thenReturn(
                        Optional.of(notification)
                );

        NotificationStateException exception =
                assertThrows(
                        NotificationStateException.class,
                        () -> outboxService.markFailed(
                                5L,
                                request
                        )
                );

        assertEquals(
                "A cancelled notification cannot be marked as failed.",
                exception.getMessage()
        );

        verify(repository, never())
                .saveAndFlush(any());
    }

}