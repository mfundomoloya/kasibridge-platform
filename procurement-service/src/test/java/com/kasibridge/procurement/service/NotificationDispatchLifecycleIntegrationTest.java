package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.validate-on-migrate=true",
                "spring.flyway.clean-disabled=true",

                "kasibridge.notifications.dispatch.enabled=false",
                "kasibridge.notifications.dispatch.batch-size=20",
                "kasibridge.notifications.dispatch.worker-id="
                        + "lifecycle-test-worker",
                "kasibridge.notifications.dispatch.lease-seconds=60",
                "kasibridge.notifications.max-retries=3",

                "kasibridge.notifications.retry.base-delay-seconds=60",
                "kasibridge.notifications.retry.max-delay-seconds=3600",

                "kasibridge.notifications.mock-whatsapp.enabled=true",
                "kasibridge.notifications.mock-whatsapp.force-failure=false",
                "kasibridge.notifications.whatsapp.enabled=false",

                "kasibridge.ticket-ai.auto-assessment.enabled=false",
                "kasibridge.internal.system.username=test-system",
                "kasibridge.internal.system.password=test-password"
        }
)
@Testcontainers
@SuppressWarnings({
        "resource",
        "SpringBootApplicationProperties"
})
class NotificationDispatchLifecycleIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "notification_lifecycle_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Autowired
    private NotificationOutboxRepository repository;

    @Autowired
    private MockWhatsAppProvider mockWhatsAppProvider;

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.flush();

        ReflectionTestUtils.setField(
                mockWhatsAppProvider,
                "enabled",
                true
        );

        ReflectionTestUtils.setField(
                mockWhatsAppProvider,
                "forceFailure",
                false
        );
    }

    @Test
    void dispatchPending_shouldCompleteSuccessfulLifecycle() {
        NotificationOutbox pendingNotification =
                savePendingNotification(
                        "KB-NOTIF-LIFECYCLE-SUCCESS",
                        201L,
                        "+27610000201",
                        "Successful lifecycle integration test."
                );

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        assertNotNull(response);

        assertEquals(
                1,
                response.getSelected()
        );

        assertEquals(
                1,
                response.getProcessed()
        );

        assertEquals(
                1,
                response.getSent()
        );

        assertEquals(
                0,
                response.getFailed()
        );

        assertEquals(
                0,
                response.getSkipped()
        );

        assertEquals(
                1,
                response.getSentNotificationIds()
                        .size()
        );

        assertEquals(
                pendingNotification.getId(),
                response.getSentNotificationIds()
                        .getFirst()
        );

        NotificationOutbox stored =
                repository.findById(
                                pendingNotification.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .SENT,
                stored.getStatus()
        );

        assertEquals(
                1,
                stored.getRetryCount()
        );

        assertNotNull(
                stored.getSentAt()
        );

        assertNotNull(
                stored.getProviderMessageId()
        );

        assertTrue(
                stored.getProviderMessageId()
                        .startsWith(
                                "MOCK-WA-"
                        )
        );

        assertEquals(
                NotificationOutbox
                        .DeliveryStatus
                        .ACCEPTED,
                stored.getDeliveryStatus()
        );

        assertNotNull(
                stored.getProviderStatusTimestamp()
        );

        assertNull(
                stored.getFailureReason()
        );
    }

    @Test
    void dispatchPending_shouldScheduleRetryAfterProviderFailure() {
        ReflectionTestUtils.setField(
                mockWhatsAppProvider,
                "forceFailure",
                true
        );

        NotificationOutbox pendingNotification =
                savePendingNotification(
                        "KB-NOTIF-LIFECYCLE-FAILURE",
                        202L,
                        "+27610000202",
                        "Failed lifecycle integration test."
                );

        LocalDateTime beforeDispatch =
                LocalDateTime.now();

        NotificationDispatchResponse response =
                dispatcherService.dispatchPending();

        LocalDateTime afterDispatch =
                LocalDateTime.now();

        assertNotNull(response);

        assertEquals(
                1,
                response.getSelected()
        );

        assertEquals(
                1,
                response.getProcessed()
        );

        assertEquals(
                0,
                response.getSent()
        );

        assertEquals(
                1,
                response.getFailed()
        );

        assertEquals(
                0,
                response.getSkipped()
        );

        assertEquals(
                1,
                response.getFailedNotificationIds()
                        .size()
        );

        assertEquals(
                pendingNotification.getId(),
                response.getFailedNotificationIds()
                        .getFirst()
        );

        NotificationOutbox stored =
                repository.findById(
                                pendingNotification.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .FAILED,
                stored.getStatus()
        );

        assertEquals(
                1,
                stored.getRetryCount()
        );

        assertNotNull(
                stored.getFailureReason()
        );

        assertFalse(
                stored.getFailureReason()
                        .isBlank()
        );

        assertTrue(
                stored.getFailureReason()
                        .contains(
                                "Mock WhatsApp provider failure"
                        )
        );

        assertNotNull(
                stored.getNextAttemptAt()
        );

        assertFalse(
                stored.getNextAttemptAt()
                        .isBefore(
                                beforeDispatch.plusSeconds(60)
                        )
        );

        assertFalse(
                stored.getNextAttemptAt()
                        .isAfter(
                                afterDispatch.plusSeconds(60)
                        )
        );

        assertNull(
                stored.getClaimedAt()
        );

        assertNull(
                stored.getClaimedBy()
        );

        assertNull(
                stored.getClaimExpiresAt()
        );

        assertNull(
                stored.getProviderMessageId()
        );

        assertNull(
                stored.getSentAt()
        );
    }

    private NotificationOutbox savePendingNotification(
            String reference,
            Long recipientUserId,
            String recipientPhone,
            String message
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .notificationReference(reference)
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
                                        .PENDING
                        )
                        .recipientUserId(recipientUserId)
                        .recipientPhone(recipientPhone)
                        .message(message)
                        .retryCount(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        return repository.saveAndFlush(
                notification
        );
    }
}