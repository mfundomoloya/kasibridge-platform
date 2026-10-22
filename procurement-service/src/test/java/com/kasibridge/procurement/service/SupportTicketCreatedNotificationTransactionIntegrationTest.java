package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.CreateSupportTicketRequest;
import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.dto.SupportTicketResponse;
import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                        + "support-ticket-test-worker",
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
class SupportTicketCreatedNotificationTransactionIntegrationTest {

    private static final Long CURRENT_USER_ID = 8L;

    private static final Long TRADER_PROFILE_ID = 2L;

    @Autowired
    private MockWhatsAppProvider mockWhatsAppProvider;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "support_ticket_notification_test"
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
    private SupportTicketService supportTicketService;

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Autowired
    private SupportTicketRepository ticketRepository;

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

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private TraderProfileClient traderProfileClient;

    @MockitoSpyBean
    private NotificationOutboxService
            notificationOutboxService;

    private Tender publishedTender;

    private TraderProfileClientResponse trader;

    @BeforeEach
    void setUp() {
        reset(
                currentUserService,
                traderProfileClient
        );

        clearInvocations(
                notificationOutboxService
        );

        notificationOutboxRepository.deleteAll();
        ticketRepository.deleteAll();
        tenderRepository.deleteAll();

        auditTestCleaner.clearAuditEvents();

        publishedTender =
                tenderRepository.saveAndFlush(
                        buildPublishedTender()
                );

        trader =
                buildTraderProfile();

        when(
                currentUserService.getCurrentUserId()
        ).thenReturn(
                CURRENT_USER_ID
        );

        when(
                traderProfileClient
                        .getCurrentTraderProfile()
        ).thenReturn(
                trader
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
    void createTicket_shouldQueueAndDispatchWhatsAppAcknowledgement() {
        SupportTicketResponse response =
                supportTicketService.createTicket(
                        publishedTender.getId(),
                        validCreateTicketRequest()
                );

        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getTicketReference());

        assertTrue(
                response.getTicketReference()
                        .startsWith("KB-TICKET-")
        );

        assertEquals(
                SupportTicket.TicketStatus.OPEN,
                response.getStatus()
        );

        List<NotificationOutbox> notifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                response.getId().equals(
                                        notification
                                                .getRelatedTicketId()
                                )
                        )
                        .toList();

        assertEquals(
                1,
                notifications.size()
        );

        NotificationOutbox notification =
                notifications.getFirst();

        assertEquals(
                NotificationOutbox
                        .NotificationTemplateType
                        .SUPPORT_TICKET_CREATED,
                notification.getTemplateType()
        );

        assertEquals(
                NotificationOutbox
                        .NotificationChannel
                        .WHATSAPP,
                notification.getChannel()
        );

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .PENDING,
                notification.getStatus()
        );

        assertEquals(
                CURRENT_USER_ID,
                notification.getRecipientUserId()
        );

        assertEquals(
                "+27610000001",
                notification.getRecipientPhone()
        );

        assertEquals(
                "biddera@kasibridge.test",
                notification.getRecipientEmail()
        );

        assertEquals(
                publishedTender.getId(),
                notification.getRelatedTenderId()
        );

        assertEquals(
                response.getId(),
                notification.getRelatedTicketId()
        );

        assertNull(
                notification.getRelatedBidId()
        );

        assertEquals(
                0,
                notification.getRetryCount()
        );

        assertNotNull(
                notification.getMessage()
        );

        assertTrue(
                notification.getMessage()
                        .contains(
                                response.getTicketReference()
                        )
        );

        assertTrue(
                notification.getMessage()
                        .contains(
                                "Sifiso Magagula"
                        )
        );

        long creationAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .count();

        assertEquals(
                1,
                creationAuditCount
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
                1,
                dispatchResponse.getSent()
        );

        assertEquals(
                0,
                dispatchResponse.getFailed()
        );

        NotificationOutbox dispatched =
                notificationOutboxRepository
                        .findById(
                                notification.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .SENT,
                dispatched.getStatus()
        );

        assertEquals(
                1,
                dispatched.getRetryCount()
        );

        assertNotNull(
                dispatched.getProviderMessageId()
        );

        assertTrue(
                dispatched.getProviderMessageId()
                        .startsWith("MOCK-WA-")
        );

        assertNotNull(
                dispatched.getSentAt()
        );

        assertEquals(
                NotificationOutbox
                        .DeliveryStatus
                        .ACCEPTED,
                dispatched.getDeliveryStatus()
        );

        assertNull(
                dispatched.getFailureReason()
        );
    }

    @Test
    void createTicket_shouldRollBackWhenRequiredOutboxInsertFails() {
        long ticketCountBefore =
                ticketRepository.count();

        long auditCountBefore =
                auditEventRepository.count();

        long outboxCountBefore =
                notificationOutboxRepository.count();

        doThrow(
                new IllegalStateException(
                        "Simulated SUPPORT_TICKET_CREATED "
                                + "outbox insert failure"
                )
        ).when(
                notificationOutboxService
        ).queueNotification(
                eq(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP
                ),
                eq(
                        NotificationOutbox
                                .NotificationTemplateType
                                .SUPPORT_TICKET_CREATED
                ),
                eq(CURRENT_USER_ID),
                eq("+27610000001"),
                eq("biddera@kasibridge.test"),
                any(String.class),
                eq(publishedTender.getId()),
                nullable(Long.class),
                any(Long.class)
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                supportTicketService
                                        .createTicket(
                                                publishedTender
                                                        .getId(),
                                                validCreateTicketRequest()
                                        )
                );

        assertEquals(
                "Simulated SUPPORT_TICKET_CREATED "
                        + "outbox insert failure",
                exception.getMessage()
        );

        assertEquals(
                ticketCountBefore,
                ticketRepository.count()
        );

        assertEquals(
                auditCountBefore,
                auditEventRepository.count()
        );

        assertEquals(
                outboxCountBefore,
                notificationOutboxRepository.count()
        );

        long survivingCreationAudits =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .count();

        assertEquals(
                0,
                survivingCreationAudits
        );
    }

    @Test
    void createTicket_shouldCommitTicketAndScheduleRetryWhenWhatsAppFails() {
        ReflectionTestUtils.setField(
                mockWhatsAppProvider,
                "forceFailure",
                true
        );

        SupportTicketResponse response =
                supportTicketService.createTicket(
                        publishedTender.getId(),
                        validCreateTicketRequest()
                );

        assertNotNull(response);
        assertNotNull(response.getId());

        SupportTicket persistedTicket =
                ticketRepository.findById(
                                response.getId()
                        )
                        .orElseThrow();

        assertEquals(
                SupportTicket.TicketStatus.OPEN,
                persistedTicket.getStatus()
        );

        List<NotificationOutbox> queuedNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                response.getId().equals(
                                        notification
                                                .getRelatedTicketId()
                                )
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
                        .NotificationTemplateType
                        .SUPPORT_TICKET_CREATED,
                queuedNotification.getTemplateType()
        );

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

        assertEquals(
                1,
                ticketRepository.count()
        );

        long creationAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .count();

        assertEquals(
                1,
                creationAuditCount
        );
    }

    private Tender buildPublishedTender() {
        return Tender.builder()
                .tenderReference(
                        "KB-TENDER-SUPPORT-TX-001"
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
                        "MUNICIPALITY-SUPPORT-TEST"
                )
                .requiredBusinessType(
                        "Road Rehabilitation and Maintenance"
                )
                .serviceArea(
                        "Gauteng"
                )
                .createdByUserId(51L)
                .status(
                        Tender.TenderStatus.PUBLISHED
                )
                .build();
    }

    private TraderProfileClientResponse
    buildTraderProfile() {
        TraderProfileClientResponse profile =
                new TraderProfileClientResponse();

        profile.setId(
                TRADER_PROFILE_ID
        );

        profile.setUserId(
                CURRENT_USER_ID
        );

        profile.setFullName(
                "Sifiso Magagula"
        );

        profile.setPhoneNumber(
                "+27610000001"
        );

        profile.setEmail(
                "biddera@kasibridge.test"
        );

        profile.setBusinessName(
                "Apex Roadworks"
        );

        profile.setBusinessType(
                "Road Rehabilitation "
                        + "and Maintenance"
        );

        profile.setTradingArea(
                "Gauteng"
        );

        return profile;
    }

    private CreateSupportTicketRequest
    validCreateTicketRequest() {
        CreateSupportTicketRequest request =
                new CreateSupportTicketRequest();

        request.setTicketType(
                SupportTicket.TicketType
                        .GENERAL_SUPPORT
        );

        request.setSubject(
                "Unable to view submitted bid"
        );

        request.setDescription(
                "A submitted bid is not visible "
                        + "on the trader dashboard."
        );

        request.setBidId(null);

        return request;
    }
}