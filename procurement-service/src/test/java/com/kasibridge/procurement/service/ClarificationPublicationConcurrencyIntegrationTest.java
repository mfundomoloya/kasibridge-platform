package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.SupportTicketAiAssessmentResponse;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.exception.TicketAiAssessmentStateException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.repository.SupportTicketAiAssessmentRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
import com.kasibridge.procurement.testsupport.ProcurementAuditTestCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
class ClarificationPublicationConcurrencyIntegrationTest {

    private static final Long APPROVER_USER_ID = 51L;
    private static final Long FIRST_PUBLISHER_USER_ID = 73L;
    private static final Long SECOND_PUBLISHER_USER_ID = 74L;
    private static final Long TRADER_USER_ID = 91L;
    private static final Long TENDER_ID = 301L;
    @Autowired
    private ProcurementAuditTestCleaner auditTestCleaner;
    private final ThreadLocal<Long> currentActorUserId =
            new ThreadLocal<>();

    private ExecutorService executorService;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "clarification_concurrency_test"
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
    private SupportTicketAiAssessmentService
            assessmentService;

    @Autowired
    private SupportTicketRepository
            ticketRepository;

    @Autowired
    private SupportTicketAiAssessmentRepository
            assessmentRepository;

    @Autowired
    private NotificationOutboxRepository
            notificationOutboxRepository;

    @Autowired
    private ProcurementAuditEventRepository
            auditEventRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        reset(currentUserService);

        notificationOutboxRepository.deleteAll();
        assessmentRepository.deleteAll();
        ticketRepository.deleteAll();
        auditTestCleaner.clearAuditEvents();

        executorService =
                Executors.newFixedThreadPool(2);

        when(currentUserService.getCurrentUserId())
                .thenAnswer(invocation -> {
                    Long actorUserId =
                            currentActorUserId.get();

                    if (actorUserId == null) {
                        throw new IllegalStateException(
                                "Concurrent test actor "
                                        + "was not configured."
                        );
                    }

                    return actorUserId;
                });

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        currentActorUserId.remove();

        executorService.shutdownNow();

        assertTrue(
                executorService.awaitTermination(
                        10,
                        TimeUnit.SECONDS
                )
        );
    }

    @Test
    void publishApprovedResponse_shouldAllowOnlyOneConcurrentPublisher()
            throws Exception {
        SupportTicket ticket =
                ticketRepository.saveAndFlush(
                        buildClarificationTicket()
                );

        SupportTicketAiAssessment assessment =
                assessmentRepository.saveAndFlush(
                        buildApprovedAssessment(
                                ticket.getId()
                        )
                );

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        Future<PublicationAttempt> firstFuture =
                executorService.submit(
                        () -> publishConcurrently(
                                assessment.getId(),
                                FIRST_PUBLISHER_USER_ID,
                                readyLatch,
                                startLatch
                        )
                );

        Future<PublicationAttempt> secondFuture =
                executorService.submit(
                        () -> publishConcurrently(
                                assessment.getId(),
                                SECOND_PUBLISHER_USER_ID,
                                readyLatch,
                                startLatch
                        )
                );

        assertTrue(
                readyLatch.await(
                        10,
                        TimeUnit.SECONDS
                ),
                "Both publication requests should "
                        + "be ready before release."
        );

        startLatch.countDown();

        PublicationAttempt firstAttempt =
                firstFuture.get(
                        30,
                        TimeUnit.SECONDS
                );

        PublicationAttempt secondAttempt =
                secondFuture.get(
                        30,
                        TimeUnit.SECONDS
                );

        List<PublicationAttempt> attempts =
                List.of(
                        firstAttempt,
                        secondAttempt
                );

        List<PublicationAttempt> successfulAttempts =
                attempts.stream()
                        .filter(
                                PublicationAttempt::successful
                        )
                        .toList();

        List<PublicationAttempt> failedAttempts =
                attempts.stream()
                        .filter(
                                attempt ->
                                        !attempt.successful()
                        )
                        .toList();

        assertEquals(
                1,
                successfulAttempts.size()
        );

        assertEquals(
                1,
                failedAttempts.size()
        );

        PublicationAttempt successfulAttempt =
                successfulAttempts.getFirst();

        PublicationAttempt failedAttempt =
                failedAttempts.getFirst();

        assertNotNull(
                successfulAttempt.response()
        );

        assertNull(
                successfulAttempt.failure()
        );

        assertNull(
                failedAttempt.response()
        );

        assertInstanceOf(
                TicketAiAssessmentStateException.class,
                failedAttempt.failure()
        );

        assertEquals(
                "Only APPROVED or EDITED_AND_APPROVED "
                        + "assessments can be published.",
                failedAttempt.failure().getMessage()
        );

        SupportTicket persistedTicket =
                ticketRepository.findById(
                        ticket.getId()
                ).orElseThrow();

        SupportTicketAiAssessment persistedAssessment =
                assessmentRepository.findById(
                        assessment.getId()
                ).orElseThrow();

        assertEquals(
                SupportTicket.TicketStatus.RESPONDED,
                persistedTicket.getStatus()
        );

        assertTrue(
                persistedTicket.isPublicClarification()
        );

        assertEquals(
                "Phased delivery is permitted "
                        + "subject to the final deadline.",
                persistedTicket.getResponse()
        );

        assertNotNull(
                persistedTicket.getRespondedAt()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .RESPONSE_PUBLISHED,
                persistedAssessment.getAssessmentStatus()
        );

        assertNotNull(
                persistedAssessment.getPublishedAt()
        );

        assertEquals(
                successfulAttempt.actorUserId(),
                persistedAssessment.getPublishedByUserId()
        );

        assertEquals(
                successfulAttempt.actorUserId(),
                persistedTicket.getRespondedByUserId()
        );

        assertTrue(
                List.of(
                        FIRST_PUBLISHER_USER_ID,
                        SECOND_PUBLISHER_USER_ID
                ).contains(
                        persistedAssessment
                                .getPublishedByUserId()
                )
        );

        assertEquals(
                1,
                countAuditEvents(
                        ProcurementAuditEvent
                                .AuditEventType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                )
        );

        assertEquals(
                1,
                countAuditEvents(
                        ProcurementAuditEvent
                                .AuditEventType
                                .SUPPORT_TICKET_AI_RESPONSE_PUBLISHED
                )
        );

        /*
         * The ticket creator receives the direct
         * ticket-response notification.
         */
        assertEquals(
                1,
                countNotifications(
                        ticket.getId(),
                        TRADER_USER_ID,
                        NotificationOutbox
                                .NotificationTemplateType
                                .SUPPORT_TICKET_RESPONDED
                )
        );

        /*
         * This fixture creates no bids. There are
         * therefore no additional bidder-wide
         * clarification broadcast recipients.
         */
        assertEquals(
                0,
                countNotificationsByTemplate(
                        ticket.getId(),
                        NotificationOutbox
                                .NotificationTemplateType
                                .OFFICIAL_CLARIFICATION_PUBLISHED
                )
        );
    }

    private PublicationAttempt publishConcurrently(
            Long assessmentId,
            Long actorUserId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {
        currentActorUserId.set(
                actorUserId
        );

        readyLatch.countDown();

        try {
            if (
                    !startLatch.await(
                            10,
                            TimeUnit.SECONDS
                    )
            ) {
                return PublicationAttempt.failure(
                        actorUserId,
                        new IllegalStateException(
                                "Concurrent publication "
                                        + "start was not released."
                        )
                );
            }

            SupportTicketAiAssessmentResponse response =
                    assessmentService
                            .publishApprovedResponse(
                                    assessmentId
                            );

            return PublicationAttempt.success(
                    actorUserId,
                    response
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return PublicationAttempt.failure(
                    actorUserId,
                    exception
            );
        } catch (RuntimeException exception) {
            return PublicationAttempt.failure(
                    actorUserId,
                    exception
            );
        } finally {
            currentActorUserId.remove();
        }
    }

    private long countAuditEvents(
            ProcurementAuditEvent.AuditEventType
                    eventType
    ) {
        return auditEventRepository
                .findAll()
                .stream()
                .filter(event ->
                        event.getEventType()
                                == eventType
                )
                .count();
    }

    private long countNotifications(
            Long ticketId,
            Long recipientUserId,
            NotificationOutbox.NotificationTemplateType
                    templateType
    ) {
        return notificationOutboxRepository
                .findAll()
                .stream()
                .filter(notification ->
                        notification.getTemplateType()
                                == templateType
                                && ticketId.equals(
                                notification
                                        .getRelatedTicketId()
                        )
                                && recipientUserId.equals(
                                notification
                                        .getRecipientUserId()
                        )
                )
                .count();
    }

    private long countNotificationsByTemplate(
            Long ticketId,
            NotificationOutbox.NotificationTemplateType
                    templateType
    ) {
        return notificationOutboxRepository
                .findAll()
                .stream()
                .filter(notification ->
                        notification.getTemplateType()
                                == templateType
                                && ticketId.equals(
                                notification
                                        .getRelatedTicketId()
                        )
                )
                .count();
    }

    private SupportTicket buildClarificationTicket() {
        return SupportTicket.builder()
                .ticketReference(
                        "KB-TICKET-CONCURRENT"
                )
                .tenderId(TENDER_ID)
                .createdByUserId(
                        TRADER_USER_ID
                )
                .contactName(
                        "Concurrency Test Trader"
                )
                .contactPhoneNumber(
                        "+27610000091"
                )
                .contactEmail(
                        "concurrency-test@example.test"
                )
                .ticketType(
                        SupportTicket.TicketType
                                .CLARIFICATION_REQUEST
                )
                .status(
                        SupportTicket.TicketStatus
                                .IN_REVIEW
                )
                .subject(
                        "Concurrent clarification test"
                )
                .description(
                        "Can delivery be completed "
                                + "in approved phases?"
                )
                .assignedToUserId(
                        FIRST_PUBLISHER_USER_ID
                )
                .assignedByUserId(11L)
                .reviewedByUserId(
                        FIRST_PUBLISHER_USER_ID
                )
                .reviewStartedAt(
                        LocalDateTime.now()
                                .minusMinutes(10)
                )
                .publicClarification(false)
                .build();
    }

    private SupportTicketAiAssessment
    buildApprovedAssessment(
            Long ticketId
    ) {
        LocalDateTime approvedAt =
                LocalDateTime.now()
                        .minusMinutes(5);

        return SupportTicketAiAssessment.builder()
                .assessmentReference(
                        "KB-AI-CONCURRENT"
                )
                .ticketId(ticketId)
                .detectedCategory(
                        SupportTicketAiAssessment
                                .DetectedCategory
                                .CLARIFICATION_REQUEST
                )
                .priority(
                        SupportTicketAiAssessment
                                .TicketPriority
                                .MEDIUM
                )
                .resolutionMode(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                )
                .assessmentStatus(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .APPROVED
                )
                .summary(
                        "The trader requests clarification "
                                + "on phased delivery."
                )
                .suggestedResponse(
                        "Phased delivery is permitted "
                                + "subject to the final deadline."
                )
                .approvedResponse(
                        "Phased delivery is permitted "
                                + "subject to the final deadline."
                )
                .approvedByUserId(
                        APPROVER_USER_ID
                )
                .approvedAt(approvedAt)
                .responseRelevanceConfirmed(true)
                .responseRelevanceConfirmedByUserId(
                        APPROVER_USER_ID
                )
                .responseRelevanceConfirmedAt(
                        approvedAt
                )
                .responseRelevanceConfirmationNotes(
                        "The response directly addresses "
                                + "the phased-delivery question."
                )
                .confidenceScore(
                        new BigDecimal("0.94")
                )
                .modelVersion(
                        "concurrency-test-model"
                )
                .build();
    }

    private record PublicationAttempt(
            Long actorUserId,
            SupportTicketAiAssessmentResponse response,
            Exception failure
    ) {
        private static PublicationAttempt success(
                Long actorUserId,
                SupportTicketAiAssessmentResponse response
        ) {
            return new PublicationAttempt(
                    actorUserId,
                    response,
                    null
            );
        }

        private static PublicationAttempt failure(
                Long actorUserId,
                Exception failure
        ) {
            return new PublicationAttempt(
                    actorUserId,
                    null,
                    failure
            );
        }

        private boolean successful() {
            return response != null;
        }
    }
}