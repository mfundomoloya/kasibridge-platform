package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.SupportTicketAiAssessmentResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.exception.TicketAiAssessmentStateException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.repository.SupportTicketAiAssessmentRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class ClarificationPublicationIdempotencyIntegrationTest {

    private static final Long APPROVER_USER_ID = 51L;
    private static final Long PUBLISHER_USER_ID = 73L;
    private static final Long TRADER_USER_ID = 91L;
    private static final Long TENDER_ID = 301L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "clarification_idempotency_test"
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
        auditEventRepository.deleteAll();

        when(currentUserService.getCurrentUserId())
                .thenReturn(PUBLISHER_USER_ID);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);
    }

    @Test
    void publishApprovedResponse_shouldPreventRepeatedPublication() {
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

        SupportTicketAiAssessmentResponse response =
                assessmentService
                        .publishApprovedResponse(
                                assessment.getId()
                        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .RESPONSE_PUBLISHED,
                response.getAssessmentStatus()
        );

        assertEquals(
                PUBLISHER_USER_ID,
                response.getPublishedByUserId()
        );

        assertNotNull(response.getPublishedAt());

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

        assertEquals(
                PUBLISHER_USER_ID,
                persistedTicket.getRespondedByUserId()
        );

        assertNotNull(
                persistedTicket.getRespondedAt()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .RESPONSE_PUBLISHED,
                persistedAssessment
                        .getAssessmentStatus()
        );

        assertEquals(
                PUBLISHER_USER_ID,
                persistedAssessment
                        .getPublishedByUserId()
        );

        assertNotNull(
                persistedAssessment.getPublishedAt()
        );

        long auditCountAfterFirstPublication =
                auditEventRepository.count();

        long outboxCountAfterFirstPublication =
                notificationOutboxRepository.count();

        long officialPublicationAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .OFFICIAL_CLARIFICATION_PUBLISHED
                        )
                        .count();

        long aiPublicationAuditCount =
                auditEventRepository
                        .findAll()
                        .stream()
                        .filter(event ->
                                event.getEventType()
                                        == ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_AI_RESPONSE_PUBLISHED
                        )
                        .count();

        assertEquals(
                1,
                officialPublicationAuditCount
        );

        assertEquals(
                1,
                aiPublicationAuditCount
        );

        TicketAiAssessmentStateException exception =
                assertThrows(
                        TicketAiAssessmentStateException.class,
                        () ->
                                assessmentService
                                        .publishApprovedResponse(
                                                assessment.getId()
                                        )
                );

        assertEquals(
                "Only APPROVED or EDITED_AND_APPROVED "
                        + "assessments can be published.",
                exception.getMessage()
        );

        assertEquals(
                auditCountAfterFirstPublication,
                auditEventRepository.count()
        );

        assertEquals(
                outboxCountAfterFirstPublication,
                notificationOutboxRepository.count()
        );

        SupportTicket ticketAfterReplay =
                ticketRepository.findById(
                        ticket.getId()
                ).orElseThrow();

        SupportTicketAiAssessment assessmentAfterReplay =
                assessmentRepository.findById(
                        assessment.getId()
                ).orElseThrow();

        assertEquals(
                SupportTicket.TicketStatus.RESPONDED,
                ticketAfterReplay.getStatus()
        );

        assertTrue(
                ticketAfterReplay
                        .isPublicClarification()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .RESPONSE_PUBLISHED,
                assessmentAfterReplay
                        .getAssessmentStatus()
        );

        assertEquals(
                PUBLISHER_USER_ID,
                assessmentAfterReplay
                        .getPublishedByUserId()
        );
    }

    private SupportTicket buildClarificationTicket() {
        return SupportTicket.builder()
                .ticketReference(
                        "KB-TICKET-IDEMPOTENCY"
                )
                .tenderId(TENDER_ID)
                .createdByUserId(
                        TRADER_USER_ID
                )
                .contactName(
                        "Idempotency Test Trader"
                )
                .contactPhoneNumber(
                        "+27610000091"
                )
                .contactEmail(
                        "idempotency-test@example.test"
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
                        "Clarification idempotency test"
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
                        "KB-AI-IDEMPOTENCY"
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
                        "idempotency-test-model"
                )
                .build();
    }
}