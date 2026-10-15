package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.repository.SupportTicketAiAssessmentRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
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
class ClarificationPublicationTransactionIntegrationTest {

    private static final Long APPROVER_USER_ID = 51L;
    private static final Long PUBLISHER_USER_ID = 73L;
    private static final Long TRADER_USER_ID = 91L;
    private static final Long TENDER_ID = 301L;
    @Autowired
    private ProcurementAuditTestCleaner auditTestCleaner;
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "clarification_transaction_test"
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

    @MockitoSpyBean
    private ProcurementAuditService auditService;

    @BeforeEach
    void setUp() {
        reset(currentUserService);
        clearInvocations(auditService);

        notificationOutboxRepository.deleteAll();
        assessmentRepository.deleteAll();
        ticketRepository.deleteAll();
        auditTestCleaner.clearAuditEvents();

        when(currentUserService.getCurrentUserId())
                .thenReturn(PUBLISHER_USER_ID);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);
    }

    @Test
    void publishApprovedResponse_shouldRollBackAllWritesWhenFinalStepFails() {
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

        long auditCountBefore =
                auditEventRepository.count();

        long outboxCountBefore =
                notificationOutboxRepository.count();

        doAnswer(invocation -> {
            ProcurementAuditEvent.AuditEventType
                    eventType =
                    invocation.getArgument(0);

            if (
                    eventType
                            == ProcurementAuditEvent
                            .AuditEventType
                            .SUPPORT_TICKET_AI_RESPONSE_PUBLISHED
            ) {
                throw new IllegalStateException(
                        "Simulated final publication failure"
                );
            }

            return invocation.callRealMethod();
        }).when(auditService).recordSuccess(
                any(
                        ProcurementAuditEvent
                                .AuditEventType.class
                ),
                nullable(Long.class),
                nullable(Long.class),
                nullable(Long.class),
                any(String.class),
                any(String.class)
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                assessmentService
                                        .publishApprovedResponse(
                                                assessment.getId()
                                        )
                );

        assertEquals(
                "Simulated final publication failure",
                exception.getMessage()
        );

        SupportTicket persistedTicket =
                ticketRepository.findById(
                        ticket.getId()
                ).orElseThrow();

        SupportTicketAiAssessment
                persistedAssessment =
                assessmentRepository.findById(
                        assessment.getId()
                ).orElseThrow();

        assertEquals(
                SupportTicket.TicketStatus.IN_REVIEW,
                persistedTicket.getStatus()
        );

        assertNull(
                persistedTicket.getResponse()
        );

        assertNull(
                persistedTicket.getRespondedByUserId()
        );

        assertNull(
                persistedTicket.getRespondedAt()
        );

        assertFalse(
                persistedTicket
                        .isPublicClarification()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .APPROVED,
                persistedAssessment
                        .getAssessmentStatus()
        );

        assertEquals(
                APPROVER_USER_ID,
                persistedAssessment
                        .getApprovedByUserId()
        );

        assertNull(
                persistedAssessment
                        .getPublishedByUserId()
        );

        assertNull(
                persistedAssessment
                        .getPublishedAt()
        );

        assertEquals(
                auditCountBefore,
                auditEventRepository.count()
        );

        assertEquals(
                outboxCountBefore,
                notificationOutboxRepository.count()
        );

        List<ProcurementAuditEvent>
                survivingPublicationAudits =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .OFFICIAL_CLARIFICATION_PUBLISHED
                                        || event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_AI_RESPONSE_PUBLISHED
                        )
                        .toList();

        assertEquals(
                0,
                survivingPublicationAudits.size()
        );

        List<NotificationOutbox>
                survivingNotifications =
                notificationOutboxRepository
                        .findAll()
                        .stream()
                        .filter(notification ->
                                ticket.getId().equals(
                                        notification
                                                .getRelatedTicketId()
                                )
                        )
                        .toList();

        assertEquals(
                0,
                survivingNotifications.size()
        );
    }

    private SupportTicket buildClarificationTicket() {
        return SupportTicket.builder()
                .ticketReference(
                        "KB-TICKET-TX-ROLLBACK"
                )
                .tenderId(TENDER_ID)
                .createdByUserId(
                        TRADER_USER_ID
                )
                .contactName(
                        "Transaction Test Trader"
                )
                .contactPhoneNumber(
                        "+27610000091"
                )
                .contactEmail(
                        "transaction-test@example.test"
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
                        "Clarification transaction test"
                )
                .description(
                        "Can delivery be completed "
                                + "in approved phases?"
                )
                .assignedToUserId(
                        PUBLISHER_USER_ID
                )
                .assignedByUserId(11L)
                .reviewedByUserId(
                        PUBLISHER_USER_ID
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
                        "KB-AI-TX-ROLLBACK"
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
                        "transaction-test-model"
                )
                .build();
    }
}