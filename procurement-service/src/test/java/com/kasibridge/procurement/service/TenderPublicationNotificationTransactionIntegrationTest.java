package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import com.kasibridge.procurement.testsupport.ProcurementAuditTestCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
                        + "tender-publication-test-worker",
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
class TenderPublicationNotificationTransactionIntegrationTest {

    private static final String BUSINESS_TYPE =
            "Road Rehabilitation and Maintenance";

    private static final String SERVICE_AREA =
            "Gauteng";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "tender_publication_notification_test"
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
    }

    @Autowired
    private TenderService tenderService;

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Autowired
    private TenderRepository tenderRepository;

    @Autowired
    private NotificationOutboxRepository
            notificationOutboxRepository;

    @Autowired
    private ProcurementAuditEventRepository
            auditEventRepository;

    @Autowired
    private ProcurementAuditTestCleaner
            auditTestCleaner;

    @Autowired
    private MockWhatsAppProvider mockWhatsAppProvider;

    @MockitoBean
    private TraderProfileClient traderProfileClient;

    @MockitoSpyBean
    private NotificationOutboxService
            notificationOutboxService;

    private Tender draftTender;

    private TraderProfileClientResponse firstTrader;

    private TraderProfileClientResponse secondTrader;

    @BeforeEach
    void setUp() {
        reset(traderProfileClient);

        clearInvocations(
                notificationOutboxService
        );

        notificationOutboxRepository.deleteAll();
        tenderRepository.deleteAll();
        auditTestCleaner.clearAuditEvents();

        draftTender =
                tenderRepository.saveAndFlush(
                        buildDraftTender()
                );

        firstTrader =
                buildTrader(
                        2L,
                        8L,
                        "Sifiso Magagula",
                        "+27610000001",
                        "biddera@kasibridge.test"
                );

        secondTrader =
                buildTrader(
                        3L,
                        9L,
                        "Naledi Mokoena",
                        "+27610000002",
                        "bidderb@kasibridge.test"
                );

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
    void publishTender_shouldQueueAndDispatchMatchingTraderNotifications() {
        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                BUSINESS_TYPE,
                                SERVICE_AREA
                        )
        ).thenReturn(
                List.of(
                        firstTrader,
                        secondTrader
                )
        );

        TenderResponse response =
                tenderService.publishTender(
                        draftTender.getId()
                );

        assertNotNull(response);

        assertEquals(
                Tender.TenderStatus.PUBLISHED,
                response.getStatus()
        );

        assertEquals(
                BUSINESS_TYPE,
                response.getRequiredBusinessType()
        );

        assertEquals(
                SERVICE_AREA,
                response.getServiceArea()
        );

        assertNotNull(
                response.getSpecificationHash()
        );

        assertNotNull(
                response.getPublishedAt()
        );

        List<NotificationOutbox> queuedNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                draftTender.getId().equals(
                                        notification
                                                .getRelatedTenderId()
                                )
                        )
                        .filter(notification ->
                                notification.getTemplateType()
                                        == NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .toList();

        assertEquals(
                2,
                queuedNotifications.size()
        );

        Set<Long> recipientUserIds =
                queuedNotifications
                        .stream()
                        .map(
                                NotificationOutbox
                                        ::getRecipientUserId
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                Set.of(
                        8L,
                        9L
                ),
                recipientUserIds
        );

        assertTrue(
                queuedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getChannel()
                                        == NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
        );

        assertTrue(
                queuedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getStatus()
                                        == NotificationOutbox
                                        .NotificationStatus
                                        .PENDING
                        )
        );

        assertTrue(
                queuedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getRelatedBidId()
                                        == null
                                        && notification
                                        .getRelatedTicketId()
                                        == null
                        )
        );

        assertTrue(
                queuedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getMessage()
                                        != null
                                        && notification
                                        .getMessage()
                                        .contains(
                                                draftTender
                                                        .getTenderReference()
                                        )
                        )
        );

        assertTrue(
                queuedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getMessage()
                                        .contains(
                                                draftTender.getTitle()
                                        )
                        )
        );

        long publicationAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .TENDER_PUBLISHED
                        )
                        .filter(event ->
                                draftTender.getId().equals(
                                        event.getTenderId()
                                )
                        )
                        .count();

        assertEquals(
                1,
                publicationAuditCount
        );

        NotificationDispatchResponse dispatchResponse =
                dispatcherService.dispatchPending();

        assertEquals(
                2,
                dispatchResponse.getSelected()
        );

        assertEquals(
                2,
                dispatchResponse.getProcessed()
        );

        assertEquals(
                2,
                dispatchResponse.getSent()
        );

        assertEquals(
                0,
                dispatchResponse.getFailed()
        );

        assertEquals(
                0,
                dispatchResponse.getSkipped()
        );

        List<NotificationOutbox> sentNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                draftTender.getId().equals(
                                        notification
                                                .getRelatedTenderId()
                                )
                        )
                        .filter(notification ->
                                notification.getTemplateType()
                                        == NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .toList();

        assertEquals(
                2,
                sentNotifications.size()
        );

        assertTrue(
                sentNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getStatus()
                                        == NotificationOutbox
                                        .NotificationStatus
                                        .SENT
                        )
        );

        assertTrue(
                sentNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getRetryCount()
                                        == 1
                        )
        );

        assertTrue(
                sentNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getProviderMessageId()
                                        != null
                                        && notification
                                        .getProviderMessageId()
                                        .startsWith(
                                                "MOCK-WA-"
                                        )
                        )
        );

        assertTrue(
                sentNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getSentAt()
                                        != null
                        )
        );

        assertTrue(
                sentNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getDeliveryStatus()
                                        == NotificationOutbox
                                        .DeliveryStatus
                                        .ACCEPTED
                        )
        );
    }

    @Test
    void publishTender_shouldRollBackWhenRequiredOutboxInsertFails() {
        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                BUSINESS_TYPE,
                                SERVICE_AREA
                        )
        ).thenReturn(
                List.of(firstTrader)
        );

        doThrow(
                new IllegalStateException(
                        "Simulated matching-tender "
                                + "outbox insert failure"
                )
        ).when(
                notificationOutboxService
        ).queueMatchingTenderNotification(
                eq(
                        firstTrader.getUserId()
                ),
                eq(
                        firstTrader.getPhoneNumber()
                ),
                eq(
                        firstTrader.getEmail()
                ),
                any(String.class),
                eq(
                        draftTender.getId()
                ),
                eq(
                        firstTrader.getFullName()
                ),
                eq(
                        draftTender.getTenderReference()
                ),
                eq(
                        draftTender.getTitle()
                )
        );

        long auditCountBefore =
                auditEventRepository.count();

        long outboxCountBefore =
                notificationOutboxRepository.count();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                tenderService.publishTender(
                                        draftTender.getId()
                                )
                );

        assertEquals(
                "Simulated matching-tender "
                        + "outbox insert failure",
                exception.getMessage()
        );

        Tender persistedTender =
                tenderRepository
                        .findById(
                                draftTender.getId()
                        )
                        .orElseThrow();

        assertEquals(
                Tender.TenderStatus.DRAFT,
                persistedTender.getStatus()
        );

        assertNull(
                persistedTender.getSpecificationHash()
        );

        assertNull(
                persistedTender.getPublishedAt()
        );

        assertEquals(
                auditCountBefore,
                auditEventRepository.count()
        );

        assertEquals(
                outboxCountBefore,
                notificationOutboxRepository.count()
        );

        long survivingPublicationAudits =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .TENDER_PUBLISHED
                        )
                        .filter(event ->
                                draftTender.getId().equals(
                                        event.getTenderId()
                                )
                        )
                        .count();

        assertEquals(
                0,
                survivingPublicationAudits
        );
    }
    @Test
    void publishTender_shouldRemainPublishedAndScheduleRetryWhenWhatsAppFails() {
        ReflectionTestUtils.setField(
                mockWhatsAppProvider,
                "forceFailure",
                true
        );

        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                BUSINESS_TYPE,
                                SERVICE_AREA
                        )
        ).thenReturn(
                List.of(firstTrader)
        );

        TenderResponse response =
                tenderService.publishTender(
                        draftTender.getId()
                );

        assertEquals(
                Tender.TenderStatus.PUBLISHED,
                response.getStatus()
        );

        assertNotNull(
                response.getSpecificationHash()
        );

        assertNotNull(
                response.getPublishedAt()
        );

        List<NotificationOutbox> queuedNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                draftTender.getId().equals(
                                        notification
                                                .getRelatedTenderId()
                                )
                        )
                        .filter(notification ->
                                notification.getTemplateType()
                                        == NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .toList();

        assertEquals(
                1,
                queuedNotifications.size()
        );

        NotificationOutbox queuedNotification =
                queuedNotifications.getFirst();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .PENDING,
                queuedNotification.getStatus()
        );

        NotificationDispatchResponse dispatchResponse =
                dispatcherService.dispatchPending();

        assertEquals(
                1,
                dispatchResponse.getSelected()
        );

        assertEquals(
                1,
                dispatchResponse.getProcessed()
        );

        assertEquals(
                0,
                dispatchResponse.getSent()
        );

        assertEquals(
                1,
                dispatchResponse.getFailed()
        );

        assertEquals(
                List.of(
                        queuedNotification.getId()
                ),
                dispatchResponse
                        .getFailedNotificationIds()
        );

        Tender persistedTender =
                tenderRepository
                        .findById(
                                draftTender.getId()
                        )
                        .orElseThrow();

        assertEquals(
                Tender.TenderStatus.PUBLISHED,
                persistedTender.getStatus()
        );

        assertNotNull(
                persistedTender.getSpecificationHash()
        );

        assertNotNull(
                persistedTender.getPublishedAt()
        );

        NotificationOutbox failedNotification =
                notificationOutboxRepository
                        .findById(
                                queuedNotification.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .FAILED,
                failedNotification.getStatus()
        );

        assertEquals(
                1,
                failedNotification.getRetryCount()
        );

        assertNotNull(
                failedNotification.getFailureReason()
        );

        assertTrue(
                failedNotification
                        .getFailureReason()
                        .contains(
                                "Mock WhatsApp provider failure"
                        )
        );

        assertNotNull(
                failedNotification.getNextAttemptAt()
        );

        assertTrue(
                failedNotification
                        .getNextAttemptAt()
                        .isAfter(
                                failedNotification
                                        .getUpdatedAt()
                                        .minusSeconds(1)
                        )
        );

        assertNull(
                failedNotification.getClaimedAt()
        );

        assertNull(
                failedNotification.getClaimedBy()
        );

        assertNull(
                failedNotification.getClaimExpiresAt()
        );

        assertNull(
                failedNotification.getProviderMessageId()
        );

        assertNull(
                failedNotification.getSentAt()
        );

        long publicationAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .TENDER_PUBLISHED
                        )
                        .filter(event ->
                                draftTender.getId().equals(
                                        event.getTenderId()
                                )
                        )
                        .count();

        assertEquals(
                1,
                publicationAuditCount
        );
    }

    @Test
    void publishTender_shouldRollBackWhenMatchingTraderLookupFails() {
        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                BUSINESS_TYPE,
                                SERVICE_AREA
                        )
        ).thenThrow(
                new IllegalStateException(
                        "Matching trader service unavailable"
                )
        );

        long auditCountBefore =
                auditEventRepository.count();

        long outboxCountBefore =
                notificationOutboxRepository.count();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                tenderService.publishTender(
                                        draftTender.getId()
                                )
                );

        assertEquals(
                "Matching trader service unavailable",
                exception.getMessage()
        );

        Tender persistedTender =
                tenderRepository
                        .findById(
                                draftTender.getId()
                        )
                        .orElseThrow();

        assertEquals(
                Tender.TenderStatus.DRAFT,
                persistedTender.getStatus()
        );

        assertNull(
                persistedTender.getSpecificationHash()
        );

        assertNull(
                persistedTender.getPublishedAt()
        );

        assertEquals(
                auditCountBefore,
                auditEventRepository.count()
        );

        assertEquals(
                outboxCountBefore,
                notificationOutboxRepository.count()
        );

        long survivingPublicationAudits =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .TENDER_PUBLISHED
                        )
                        .filter(event ->
                                draftTender.getId().equals(
                                        event.getTenderId()
                                )
                        )
                        .count();

        assertEquals(
                0,
                survivingPublicationAudits
        );
    }

    private Tender buildDraftTender() {
        return Tender.builder()
                .tenderReference(
                        "KB-TENDER-MATCH-TX-001"
                )
                .title(
                        "Road Rehabilitation "
                                + "and Maintenance"
                )
                .description(
                        "Road rehabilitation, pothole "
                                + "repair, drainage improvement "
                                + "and road marking services."
                )
                .evaluationCriteria(
                        "Technical capability, compliance, "
                                + "price and implementation plan."
                )
                .budgetAmount(
                        new BigDecimal(
                                "850000.00"
                        )
                )
                .buyerOrgId(
                        "MUNICIPALITY-MATCH-TEST"
                )
                .requiredBusinessType(
                        BUSINESS_TYPE
                )
                .serviceArea(
                        SERVICE_AREA
                )
                .createdByUserId(51L)
                .status(
                        Tender.TenderStatus.DRAFT
                )
                .build();
    }

    private TraderProfileClientResponse buildTrader(
            Long profileId,
            Long userId,
            String fullName,
            String phoneNumber,
            String email
    ) {
        TraderProfileClientResponse profile =
                new TraderProfileClientResponse();

        profile.setId(profileId);
        profile.setUserId(userId);
        profile.setFullName(fullName);
        profile.setPhoneNumber(phoneNumber);
        profile.setEmail(email);
        profile.setBusinessName(
                "Matching Trader " + profileId
        );
        profile.setBusinessType(
                BUSINESS_TYPE
        );
        profile.setTradingArea(
                SERVICE_AREA
        );

        return profile;
    }
}