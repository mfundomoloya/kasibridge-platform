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
    private static final Long BID_ID = 501L;

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
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );

        registry.add(
                "spring.flyway.locations",
                () -> "classpath:db/migration"
        );

        registry.add(
                "spring.flyway.validate-on-migrate",
                () -> "true"
        );

        registry.add(
                "spring.flyway.clean-disabled",
                () -> "true"
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

    @Test
    void saveAndFlush_shouldAllowSameBidEventForDifferentRecipients() {
        repository.saveAndFlush(
                buildBidNotification(
                        "KB-NOTIFY-BID-RECIPIENT-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        RECIPIENT_USER_ID
                )
        );

        repository.saveAndFlush(
                buildBidNotification(
                        "KB-NOTIFY-BID-RECIPIENT-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        RECIPIENT_USER_ID + 1
                )
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    @Test
    void saveAndFlush_shouldAllowDifferentClarificationTicketsForSameBidAndRecipient() {
        NotificationOutbox firstClarification =
                buildNotification(
                        "KB-NOTIFY-CLARIFICATION-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                );

        firstClarification.setRelatedBidId(
                BID_ID
        );

        firstClarification.setRelatedTicketId(
                TICKET_ID
        );

        repository.saveAndFlush(
                firstClarification
        );

        NotificationOutbox secondClarification =
                buildNotification(
                        "KB-NOTIFY-CLARIFICATION-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                );

        secondClarification.setRelatedBidId(
                BID_ID
        );

        secondClarification.setRelatedTicketId(
                TICKET_ID + 1
        );

        repository.saveAndFlush(
                secondClarification
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    @Test
    void saveAndFlush_shouldRejectDuplicateBidEventForSameBidAndRecipient() {
        repository.saveAndFlush(
                buildBidNotification(
                        "KB-NOTIFY-BID-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        RECIPIENT_USER_ID
                )
        );

        NotificationOutbox duplicate =
                buildBidNotification(
                        "KB-NOTIFY-BID-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        RECIPIENT_USER_ID
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        duplicate
                )
        );
    }

    @Test
    void saveAndFlush_shouldAllowDifferentBidEventTemplatesForSameBidAndRecipient() {
        repository.saveAndFlush(
                buildBidNotification(
                        "KB-NOTIFY-BID-TEMPLATE-001",
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        RECIPIENT_USER_ID
                )
        );

        repository.saveAndFlush(
                buildBidNotification(
                        "KB-NOTIFY-BID-TEMPLATE-002",
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_COMPLIANCE_PASSED,
                        BID_ID,
                        RECIPIENT_USER_ID
                )
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    @Test
    void saveAndFlush_shouldRejectDuplicateMatchingTenderNotification() {
        repository.saveAndFlush(
                buildMatchingTenderNotification(
                        "KB-NOTIFY-MATCH-001",
                        TENDER_ID,
                        RECIPIENT_USER_ID
                )
        );

        NotificationOutbox duplicate =
                buildMatchingTenderNotification(
                        "KB-NOTIFY-MATCH-002",
                        TENDER_ID,
                        RECIPIENT_USER_ID
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        duplicate
                )
        );
    }

    @Test
    void saveAndFlush_shouldAllowMatchingTenderNotificationForDifferentRecipient() {
        repository.saveAndFlush(
                buildMatchingTenderNotification(
                        "KB-NOTIFY-MATCH-RECIPIENT-001",
                        TENDER_ID,
                        RECIPIENT_USER_ID
                )
        );

        repository.saveAndFlush(
                buildMatchingTenderNotification(
                        "KB-NOTIFY-MATCH-RECIPIENT-002",
                        TENDER_ID,
                        RECIPIENT_USER_ID + 1
                )
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    @Test
    void saveAndFlush_shouldAllowDifferentTenderTemplateForSameTenderAndRecipient() {
        repository.saveAndFlush(
                buildMatchingTenderNotification(
                        "KB-NOTIFY-MATCH-TEMPLATE-001",
                        TENDER_ID,
                        RECIPIENT_USER_ID
                )
        );

        NotificationOutbox awardedNotification =
                NotificationOutbox.builder()
                        .notificationReference(
                                "KB-NOTIFY-MATCH-TEMPLATE-002"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .TENDER_AWARDED
                        )
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
                                "Tender outcome notification."
                        )
                        .relatedTenderId(
                                TENDER_ID
                        )
                        .retryCount(0)
                        .build();

        repository.saveAndFlush(
                awardedNotification
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    private NotificationOutbox
    buildMatchingTenderNotification(
            String notificationReference,
            Long tenderId,
            Long recipientUserId
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
                .templateType(
                        NotificationOutbox
                                .NotificationTemplateType
                                .MATCHING_TENDER_PUBLISHED
                )
                .status(
                        NotificationOutbox
                                .NotificationStatus
                                .PENDING
                )
                .recipientUserId(
                        recipientUserId
                )
                .recipientPhone(
                        "+27610000091"
                )
                .recipientEmail(
                        "notification-test@example.test"
                )
                .message(
                        "A matching tender was published."
                )
                .relatedTenderId(
                        tenderId
                )
                .retryCount(0)
                .build();
    }

    private NotificationOutbox buildBidNotification(
            String notificationReference,
            NotificationOutbox.NotificationTemplateType
                    templateType,
            Long bidId,
            Long recipientUserId
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
                        recipientUserId
                )
                .recipientPhone(
                        "+27610000091"
                )
                .recipientEmail(
                        "bid-notification-test@example.test"
                )
                .message(
                        "Bid lifecycle notification test."
                )
                .relatedTenderId(
                        TENDER_ID
                )
                .relatedBidId(
                        bidId
                )
                .retryCount(0)
                .build();
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