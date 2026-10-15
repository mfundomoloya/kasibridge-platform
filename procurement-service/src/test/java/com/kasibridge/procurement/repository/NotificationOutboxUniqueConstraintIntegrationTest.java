package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.NotificationOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        properties = {
                "kasibridge.notifications.dispatch.enabled=false",
                "kasibridge.notifications.mock-whatsapp.enabled=true",
                "kasibridge.notifications.whatsapp.enabled=false",
                "kasibridge.ticket-ai.auto-assessment.enabled=false",
                "kasibridge.internal.system.username=test-system",
                "kasibridge.internal.system.password=test-password"
        }
)
@Testcontainers
class NotificationOutboxUniqueConstraintIntegrationTest {

    private static final Long TICKET_ID = 401L;
    private static final Long TENDER_ID = 301L;
    private static final Long RECIPIENT_USER_ID = 91L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "notification_constraint_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

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

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "false"
        );
    }

    @Autowired
    private NotificationOutboxRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void saveAndFlush_shouldRejectDuplicateTemplateTicketAndRecipient() {
        repository.saveAndFlush(
                buildNotification(
                        "KB-NOTIFY-UNIQUE-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                )
        );

        NotificationOutbox duplicate =
                buildNotification(
                        "KB-NOTIFY-UNIQUE-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        duplicate
                )
        );
    }

    @Test
    void saveAndFlush_shouldAllowDifferentTemplateForSameTicketAndRecipient() {
        repository.saveAndFlush(
                buildNotification(
                        "KB-NOTIFY-TEMPLATE-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .SUPPORT_TICKET_RESPONDED
                )
        );

        repository.saveAndFlush(
                buildNotification(
                        "KB-NOTIFY-TEMPLATE-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                )
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    @Test
    void saveAndFlush_shouldAllowSameTemplateForDifferentRecipient() {
        repository.saveAndFlush(
                buildNotification(
                        "KB-NOTIFY-RECIPIENT-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                )
        );

        NotificationOutbox secondRecipient =
                buildNotification(
                        "KB-NOTIFY-RECIPIENT-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                );

        secondRecipient.setRecipientUserId(
                RECIPIENT_USER_ID + 1
        );

        repository.saveAndFlush(
                secondRecipient
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    private NotificationOutbox buildNotification(
            String notificationReference,
            NotificationOutbox.NotificationTemplateType
                    templateType
    ) {
        return NotificationOutbox.builder()
                .notificationReference(
                        notificationReference
                )
                .channel(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP
                )
                .templateType(templateType)
                .status(
                        NotificationOutbox
                                .NotificationStatus
                                .PENDING
                )
                .recipientUserId(
                        RECIPIENT_USER_ID
                )
                .recipientPhone(
                        "+27610000091"
                )
                .recipientEmail(
                        "notification-test@example.test"
                )
                .message(
                        "Official clarification test message."
                )
                .relatedTenderId(
                        TENDER_ID
                )
                .relatedTicketId(
                        TICKET_ID
                )
                .retryCount(0)
                .build();
    }
}