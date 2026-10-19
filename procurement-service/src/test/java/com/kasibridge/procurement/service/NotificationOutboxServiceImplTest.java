package com.kasibridge.procurement.service;

import com.kasibridge.procurement.config.NotificationRetryProperties;
import com.kasibridge.procurement.dto.MarkNotificationFailedRequest;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.exception.NotificationStateException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.Mockito.never;

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

    @Mock
    private NotificationRetryProperties retryProperties;

    @InjectMocks
    private NotificationOutboxServiceImpl outboxService;

    @BeforeEach
    void setUp() {
        lenient()
                .when(
                        retryProperties.baseDelaySeconds()
                )
                .thenReturn(60L);

        lenient()
                .when(
                        retryProperties.maxDelaySeconds()
                )
                .thenReturn(3600L);
    }

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

    @Test
    void markFailed_shouldCapBackoffAtConfiguredMaximumDelay() {
        LocalDateTime claimedAt =
                LocalDateTime.now()
                        .minusSeconds(10);

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .id(6L)
                        .notificationReference(
                                "KB-NOTIF-BACKOFF-06"
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
                        .recipientUserId(106L)
                        .recipientPhone(
                                "+27610000106"
                        )
                        .message(
                                "Maximum backoff test."
                        )
                        .retryCount(8)
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

        when(repository.findById(6L))
                .thenReturn(
                        Optional.of(notification)
                );

        when(repository.saveAndFlush(notification))
                .thenReturn(notification);

        when(retryProperties.baseDelaySeconds())
                .thenReturn(60L);

        when(retryProperties.maxDelaySeconds())
                .thenReturn(300L);

        LocalDateTime beforeFailure =
                LocalDateTime.now();

        outboxService.markFailed(
                6L,
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
                8,
                saved.getRetryCount()
        );

        assertNotNull(
                saved.getNextAttemptAt()
        );

        assertTrue(
                !saved.getNextAttemptAt()
                        .isBefore(
                                beforeFailure.plusSeconds(300)
                        )
        );

        assertTrue(
                !saved.getNextAttemptAt()
                        .isAfter(
                                afterFailure.plusSeconds(300)
                        )
        );
    }

    @Test
    void getInAppNotifications_shouldReturnAllNotificationsForCurrentTrader() {
        Long currentUserId = 201L;

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        NotificationOutbox notification =
                createInAppNotification(
                        10L,
                        currentUserId,
                        null
                );

        Page<NotificationOutbox> notificationPage =
                new PageImpl<>(
                        List.of(notification),
                        pageable,
                        1
                );

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(currentUserService.getCurrentUserId())
                .thenReturn(currentUserId);

        when(repository.findByChannelAndRecipientUserId(
                NotificationOutbox.NotificationChannel.IN_APP,
                currentUserId,
                pageable
        )).thenReturn(notificationPage);

        Page<?> result =
                outboxService.getInAppNotifications(
                        pageable
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        verify(repository)
                .findByChannelAndRecipientUserId(
                        NotificationOutbox
                                .NotificationChannel
                                .IN_APP,
                        currentUserId,
                        pageable
                );

        verify(repository, never())
                .findByChannelAndRecipientUserIdAndReadAtIsNull(
                        any(),
                        anyLong(),
                        any()
                );

        verify(repository, never())
                .findByChannelAndRecipientUserIdAndReadAtIsNotNull(
                        any(),
                        anyLong(),
                        any()
                );
    }

    @Test
    void getUnreadInAppNotifications_shouldReturnOnlyUnreadForCurrentTrader() {
        Long currentUserId = 202L;

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        NotificationOutbox unreadNotification =
                createInAppNotification(
                        11L,
                        currentUserId,
                        null
                );

        Page<NotificationOutbox> notificationPage =
                new PageImpl<>(
                        List.of(unreadNotification),
                        pageable,
                        1
                );

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(currentUserService.getCurrentUserId())
                .thenReturn(currentUserId);

        when(repository
                .findByChannelAndRecipientUserIdAndReadAtIsNull(
                        NotificationOutbox
                                .NotificationChannel
                                .IN_APP,
                        currentUserId,
                        pageable
                ))
                .thenReturn(notificationPage);

        Page<?> result =
                outboxService
                        .getUnreadInAppNotifications(
                                pageable
                        );

        assertEquals(
                1,
                result.getTotalElements()
        );

        verify(repository)
                .findByChannelAndRecipientUserIdAndReadAtIsNull(
                        NotificationOutbox
                                .NotificationChannel
                                .IN_APP,
                        currentUserId,
                        pageable
                );

        verify(repository, never())
                .findByChannelAndRecipientUserIdAndReadAtIsNotNull(
                        any(),
                        anyLong(),
                        any()
                );
    }

    @Test
    void getReadInAppNotifications_shouldReturnOnlyReadForCurrentTrader() {
        Long currentUserId = 203L;

        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        NotificationOutbox readNotification =
                createInAppNotification(
                        12L,
                        currentUserId,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        Page<NotificationOutbox> notificationPage =
                new PageImpl<>(
                        List.of(readNotification),
                        pageable,
                        1
                );

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(currentUserService.getCurrentUserId())
                .thenReturn(currentUserId);

        when(repository
                .findByChannelAndRecipientUserIdAndReadAtIsNotNull(
                        NotificationOutbox
                                .NotificationChannel
                                .IN_APP,
                        currentUserId,
                        pageable
                ))
                .thenReturn(notificationPage);

        Page<?> result =
                outboxService.getReadInAppNotifications(
                        pageable
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        verify(repository)
                .findByChannelAndRecipientUserIdAndReadAtIsNotNull(
                        NotificationOutbox
                                .NotificationChannel
                                .IN_APP,
                        currentUserId,
                        pageable
                );

        verify(repository, never())
                .findByChannelAndRecipientUserIdAndReadAtIsNull(
                        any(),
                        anyLong(),
                        any()
                );
    }

    private NotificationOutbox createInAppNotification(
            Long id,
            Long recipientUserId,
            LocalDateTime readAt
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        return NotificationOutbox.builder()
                .id(id)
                .notificationReference(
                        "KB-NOTIF-IN-APP-" + id
                )
                .channel(
                        NotificationOutbox
                                .NotificationChannel
                                .IN_APP
                )
                .templateType(
                        NotificationOutbox
                                .NotificationTemplateType
                                .SUPPORT_TICKET_RESPONDED
                )
                .status(
                        NotificationOutbox
                                .NotificationStatus
                                .SENT
                )
                .recipientUserId(recipientUserId)
                .message(
                        "In-app notification filter test."
                )
                .retryCount(1)
                .readByUserId(
                        readAt == null
                                ? null
                                : recipientUserId
                )
                .readAt(readAt)
                .createdAt(
                        now.minusMinutes(10)
                )
                .sentAt(
                        now.minusMinutes(9)
                )
                .updatedAt(now)
                .build();
    }

    @Test
    void getInAppNotifications_shouldReturnAllNotificationsForPlatformAdmin() {
        Pageable pageable =
                PageRequest.of(0, 10);

        NotificationOutbox notification =
                createInAppNotification(
                        13L,
                        204L,
                        null
                );

        Page<NotificationOutbox> notificationPage =
                new PageImpl<>(
                        List.of(notification),
                        pageable,
                        1
                );

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(true);

        when(repository.findByChannel(
                NotificationOutbox.NotificationChannel.IN_APP,
                pageable
        )).thenReturn(notificationPage);

        Page<?> result =
                outboxService.getInAppNotifications(
                        pageable
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        verify(repository).findByChannel(
                NotificationOutbox.NotificationChannel.IN_APP,
                pageable
        );

        verify(
                currentUserService,
                never()
        ).getCurrentUserId();

        verify(
                repository,
                never()
        ).findByChannelAndReadAtIsNull(
                any(),
                any()
        );

        verify(
                repository,
                never()
        ).findByChannelAndReadAtIsNotNull(
                any(),
                any()
        );
    }

    @Test
    void getUnreadInAppNotifications_shouldReturnOnlyUnreadForPlatformAdmin() {
        Pageable pageable =
                PageRequest.of(0, 10);

        NotificationOutbox notification =
                createInAppNotification(
                        14L,
                        205L,
                        null
                );

        Page<NotificationOutbox> notificationPage =
                new PageImpl<>(
                        List.of(notification),
                        pageable,
                        1
                );

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(true);

        when(repository.findByChannelAndReadAtIsNull(
                NotificationOutbox.NotificationChannel.IN_APP,
                pageable
        )).thenReturn(notificationPage);

        Page<?> result =
                outboxService.getUnreadInAppNotifications(
                        pageable
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        verify(repository)
                .findByChannelAndReadAtIsNull(
                        NotificationOutbox.NotificationChannel.IN_APP,
                        pageable
                );

        verify(
                currentUserService,
                never()
        ).getCurrentUserId();

        verify(
                repository,
                never()
        ).findByChannelAndReadAtIsNotNull(
                any(),
                any()
        );
    }

    @Test
    void getReadInAppNotifications_shouldReturnOnlyReadForPlatformAdmin() {
        Pageable pageable =
                PageRequest.of(0, 10);

        NotificationOutbox notification =
                createInAppNotification(
                        15L,
                        206L,
                        LocalDateTime.now()
                                .minusMinutes(5)
                );

        Page<NotificationOutbox> notificationPage =
                new PageImpl<>(
                        List.of(notification),
                        pageable,
                        1
                );

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(true);

        when(repository.findByChannelAndReadAtIsNotNull(
                NotificationOutbox.NotificationChannel.IN_APP,
                pageable
        )).thenReturn(notificationPage);

        Page<?> result =
                outboxService.getReadInAppNotifications(
                        pageable
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        verify(repository)
                .findByChannelAndReadAtIsNotNull(
                        NotificationOutbox.NotificationChannel.IN_APP,
                        pageable
                );

        verify(
                currentUserService,
                never()
        ).getCurrentUserId();

        verify(
                repository,
                never()
        ).findByChannelAndReadAtIsNull(
                any(),
                any()
        );
    }
}