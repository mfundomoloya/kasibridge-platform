package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.SubmitBidRequest;
import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.repository.BidComplianceResultRepository;
import com.kasibridge.procurement.repository.BidRepository;
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
import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.NotificationDispatchResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
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
                "kasibridge.notifications.mock-whatsapp.enabled=true",
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
class BidSubmissionNotificationTransactionIntegrationTest {

    private static final Long CURRENT_USER_ID = 8L;

    private static final Long TRADER_PROFILE_ID = 2L;

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "bid_notification_transaction_test"
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
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BidComplianceResultRepository
            complianceRepository;

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
        complianceRepository.deleteAll();
        bidRepository.deleteAll();
        tenderRepository.deleteAll();

        auditTestCleaner.clearAuditEvents();

        publishedTender =
                tenderRepository.saveAndFlush(
                        buildPublishedTender()
                );

        trader =
                buildTraderProfile();

        when(
                currentUserService
                        .getCurrentUserId()
        ).thenReturn(
                CURRENT_USER_ID
        );

        when(
                traderProfileClient
                        .getCurrentTraderProfile()
        ).thenReturn(
                trader
        );

        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(
                trader
        );
    }

    @Test
    void submitBid_shouldRollBackAllWritesWhenRequiredOutboxInsertFails() {
        long bidCountBefore =
                bidRepository.count();

        long complianceCountBefore =
                complianceRepository.count();

        long auditCountBefore =
                auditEventRepository.count();

        long outboxCountBefore =
                notificationOutboxRepository.count();

        doThrow(
                new IllegalStateException(
                        "Simulated BID_RECEIVED "
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
                                .BID_RECEIVED
                ),
                eq(CURRENT_USER_ID),
                eq("+27610000001"),
                eq("biddera@kasibridge.test"),
                nullable(String.class),
                eq(publishedTender.getId()),
                nullable(Long.class),
                nullable(Long.class)
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                bidService.submitBid(
                                        publishedTender.getId(),
                                        validSubmitBidRequest()
                                )
                );

        assertEquals(
                "Simulated BID_RECEIVED "
                        + "outbox insert failure",
                exception.getMessage()
        );

        assertEquals(
                bidCountBefore,
                bidRepository.count()
        );

        assertEquals(
                complianceCountBefore,
                complianceRepository.count()
        );

        assertEquals(
                auditCountBefore,
                auditEventRepository.count()
        );

        assertEquals(
                outboxCountBefore,
                notificationOutboxRepository.count()
        );

        assertEquals(
                0,
                bidRepository
                        .findByTenderId(
                                publishedTender.getId()
                        )
                        .size()
        );

        assertEquals(
                0,
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SUBMITTED
                                        || event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_COMPLIANCE_PASSED
                                        || event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_COMPLIANCE_FAILED
                        )
                        .count()
        );
    }

    @Test
    void submitBid_shouldQueueAndDispatchRequiredWhatsAppNotifications() {
        BidResponse bidResponse =
                bidService.submitBid(
                        publishedTender.getId(),
                        validSubmitBidRequest()
                );

        assertNotNull(
                bidResponse
        );

        assertNotNull(
                bidResponse.getId()
        );

        assertEquals(
                Bid.BidStatus.COMPLIANT,
                bidResponse.getStatus()
        );

        List<NotificationOutbox> queuedNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                bidResponse.getId().equals(
                                        notification
                                                .getRelatedBidId()
                                )
                        )
                        .toList();

        assertEquals(
                2,
                queuedNotifications.size()
        );

        Map<
                NotificationOutbox.NotificationTemplateType,
                NotificationOutbox
                > queuedByTemplate =
                queuedNotifications
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        NotificationOutbox
                                                ::getTemplateType,
                                        Function.identity()
                                )
                        );

        assertTrue(
                queuedByTemplate.containsKey(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED
                )
        );

        assertTrue(
                queuedByTemplate.containsKey(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_COMPLIANCE_PASSED
                )
        );

        NotificationOutbox receivedNotification =
                queuedByTemplate.get(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED
                );

        NotificationOutbox complianceNotification =
                queuedByTemplate.get(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_COMPLIANCE_PASSED
                );

        assertQueuedBidNotification(
                receivedNotification,
                bidResponse.getId()
        );

        assertQueuedBidNotification(
                complianceNotification,
                bidResponse.getId()
        );

        NotificationDispatchResponse dispatchResponse =
                dispatcherService.dispatchPending();

        assertNotNull(
                dispatchResponse
        );

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

        assertEquals(
                2,
                dispatchResponse
                        .getSentNotificationIds()
                        .size()
        );

        List<NotificationOutbox> dispatchedNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                bidResponse.getId().equals(
                                        notification
                                                .getRelatedBidId()
                                )
                        )
                        .toList();

        assertEquals(
                2,
                dispatchedNotifications.size()
        );

        assertTrue(
                dispatchedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getStatus()
                                        == NotificationOutbox
                                        .NotificationStatus
                                        .SENT
                        )
        );

        assertTrue(
                dispatchedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification
                                        .getRetryCount()
                                        == 1
                        )
        );

        assertTrue(
                dispatchedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification.getSentAt()
                                        != null
                        )
        );

        assertTrue(
                dispatchedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification
                                        .getProviderMessageId()
                                        != null
                                        && notification
                                        .getProviderMessageId()
                                        .startsWith(
                                                "MOCK-WA-"
                                        )
                        )
        );

        assertTrue(
                dispatchedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification
                                        .getDeliveryStatus()
                                        == NotificationOutbox
                                        .DeliveryStatus
                                        .ACCEPTED
                        )
        );

        assertTrue(
                dispatchedNotifications
                        .stream()
                        .allMatch(notification ->
                                notification
                                        .getFailureReason()
                                        == null
                        )
        );

        long bidAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                bidResponse.getId().equals(
                                        event.getBidId()
                                )
                        )
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SUBMITTED
                                        || event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_COMPLIANCE_PASSED
                        )
                        .count();

        assertEquals(
                2,
                bidAuditCount
        );
    }

    private Tender buildPublishedTender() {
        return Tender.builder()
                .tenderReference(
                        "KB-TENDER-BID-TX-001"
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
                                + "price and delivery readiness."
                )
                .budgetAmount(
                        new BigDecimal(
                                "850000.00"
                        )
                )
                .buyerOrgId(
                        "MUNICIPALITY-TX-TEST"
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

    private SubmitBidRequest
    validSubmitBidRequest() {
        SubmitBidRequest request =
                new SubmitBidRequest();

        request.setTechnicalProposal(
                "Technical proposal for road "
                        + "rehabilitation, drainage "
                        + "improvement and road marking."
        );

        request.setPriceAmount(
                new BigDecimal(
                        "690000.00"
                )
        );

        request.setCsdValid(true);
        request.setTaxClearanceValid(true);
        request.setBbbeeValid(true);
        request.setRequiredDocumentsUploaded(true);

        return request;
    }

    private void assertQueuedBidNotification(
            NotificationOutbox notification,
            Long bidId
    ) {
        assertNotNull(
                notification
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
                bidId,
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
                !notification.getMessage()
                        .isBlank()
        );

        assertNull(
                notification.getProviderMessageId()
        );

        assertNull(
                notification.getSentAt()
        );
    }
}