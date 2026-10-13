package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.ApproveTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.SupportTicketAiAssessmentResponse;
import com.kasibridge.procurement.dto.SupportTicketResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.exception.TicketAiAssessmentStateException;
import com.kasibridge.procurement.repository.SupportTicketAiAssessmentRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketAiAssessmentServiceImplTest {

    private static final Long ASSESSMENT_ID = 71L;
    private static final Long TICKET_ID = 42L;
    private static final Long TENDER_ID = 3L;
    private static final Long APPROVER_USER_ID = 5L;
    private static final Long PUBLISHER_USER_ID = 9L;

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private SupportTicketAiAssessmentRepository assessmentRepository;

    @Mock
    private TicketAiTriageService triageService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private SupportTicketService supportTicketService;

    @InjectMocks
    private SupportTicketAiAssessmentServiceImpl service;

    @Test
    void approveAssessment_shouldApproveGeneratedAssessment() {
        SupportTicketAiAssessment assessment =
                generatedAssessment(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                );

        SupportTicket ticket = clarificationTicket();

        ApproveTicketAiAssessmentRequest request =
                approvalRequest(
                        "Phased delivery is permitted.",
                        true,
                        "The response directly addresses "
                                + "the delivery question."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(APPROVER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        when(assessmentRepository.saveAndFlush(
                assessment
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        when(ticketRepository.findById(TICKET_ID))
                .thenReturn(Optional.of(ticket));

        SupportTicketAiAssessmentResponse response =
                service.approveAssessment(
                        ASSESSMENT_ID,
                        request
                );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .EDITED_AND_APPROVED,
                assessment.getAssessmentStatus()
        );

        assertEquals(
                "Phased delivery is permitted.",
                assessment.getApprovedResponse()
        );

        assertEquals(
                APPROVER_USER_ID,
                assessment.getApprovedByUserId()
        );

        assertNotNull(assessment.getApprovedAt());

        assertTrue(
                assessment.isResponseRelevanceConfirmed()
        );

        assertEquals(
                APPROVER_USER_ID,
                assessment
                        .getResponseRelevanceConfirmedByUserId()
        );

        assertNotNull(
                assessment
                        .getResponseRelevanceConfirmedAt()
        );

        assertEquals(
                "The response directly addresses "
                        + "the delivery question.",
                assessment
                        .getResponseRelevanceConfirmationNotes()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .EDITED_AND_APPROVED,
                response.getAssessmentStatus()
        );

        assertTrue(
                response.isResponseRelevanceConfirmed()
        );

        assertEquals(
                APPROVER_USER_ID,
                response
                        .getResponseRelevanceConfirmedByUserId()
        );

        verify(assessmentRepository)
                .saveAndFlush(assessment);

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_AI_ASSESSMENT_APPROVED
                        ),
                        eq(TENDER_ID),
                        isNull(),
                        eq(APPROVER_USER_ID),
                        eq(
                                "Support ticket AI "
                                        + "assessment approved"
                        ),
                        argThat(details ->
                                details.contains(
                                        "Assessment reference="
                                                + assessment
                                                .getAssessmentReference()
                                )
                                        && details.contains(
                                        "status="
                                                + SupportTicketAiAssessment
                                                .AiAssessmentStatus
                                                .EDITED_AND_APPROVED
                                )
                        )
                );

        verifyNoInteractions(supportTicketService);
    }

    @Test
    void approveAssessment_shouldUseSuggestedResponseWhenNoEditProvided() {
        SupportTicketAiAssessment assessment =
                generatedAssessment(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                );

        SupportTicket ticket = clarificationTicket();

        ApproveTicketAiAssessmentRequest request =
                approvalRequest(
                        null,
                        true,
                        "The proposed response is relevant."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(APPROVER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        when(assessmentRepository.saveAndFlush(
                assessment
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        when(ticketRepository.findById(TICKET_ID))
                .thenReturn(Optional.of(ticket));

        SupportTicketAiAssessmentResponse response =
                service.approveAssessment(
                        ASSESSMENT_ID,
                        request
                );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .APPROVED,
                assessment.getAssessmentStatus()
        );

        assertEquals(
                assessment.getSuggestedResponse(),
                assessment.getApprovedResponse()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .APPROVED,
                response.getAssessmentStatus()
        );
    }

    @Test
    void approveAssessment_shouldRejectMissingRelevanceConfirmation() {
        SupportTicketAiAssessment assessment =
                generatedAssessment(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                );

        ApproveTicketAiAssessmentRequest request =
                approvalRequest(
                        null,
                        false,
                        "Response reviewed but not confirmed."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(APPROVER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        TicketAiAssessmentStateException exception =
                assertThrows(
                        TicketAiAssessmentStateException.class,
                        () -> service.approveAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                );

        assertEquals(
                "The reviewer must confirm that the "
                        + "approved response addresses the "
                        + "ticket subject and description.",
                exception.getMessage()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .GENERATED,
                assessment.getAssessmentStatus()
        );

        assertFalse(
                assessment.isResponseRelevanceConfirmed()
        );

        assertNull(
                assessment
                        .getResponseRelevanceConfirmedByUserId()
        );

        verify(assessmentRepository, never())
                .saveAndFlush(
                        any(
                                SupportTicketAiAssessment.class
                        )
                );

        verifyNoInteractions(
                auditService,
                supportTicketService
        );
    }

    @Test
    void approveAssessment_shouldRejectHumanOnlyAssessment() {
        SupportTicketAiAssessment assessment =
                generatedAssessment(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .HUMAN_ONLY
                );

        ApproveTicketAiAssessmentRequest request =
                approvalRequest(
                        null,
                        true,
                        "The response was reviewed."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(APPROVER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        TicketAiAssessmentStateException exception =
                assertThrows(
                        TicketAiAssessmentStateException.class,
                        () -> service.approveAssessment(
                                ASSESSMENT_ID,
                                request
                        )
                );

        assertEquals(
                "HUMAN_ONLY assessments cannot be "
                        + "approved as AI responses.",
                exception.getMessage()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .GENERATED,
                assessment.getAssessmentStatus()
        );

        assertNull(assessment.getApprovedByUserId());
        assertNull(assessment.getApprovedAt());
        assertNull(assessment.getApprovedResponse());

        verify(assessmentRepository, never())
                .saveAndFlush(
                        any(
                                SupportTicketAiAssessment.class
                        )
                );

        verifyNoInteractions(
                auditService,
                supportTicketService
        );
    }

    @Test
    void publishApprovedResponse_shouldRejectOriginalApprover() {
        SupportTicketAiAssessment assessment =
                approvedAssessment(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .APPROVED
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(APPROVER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        TicketAiAssessmentStateException exception =
                assertThrows(
                        TicketAiAssessmentStateException.class,
                        () -> service.publishApprovedResponse(
                                ASSESSMENT_ID
                        )
                );

        assertEquals(
                "The user who approved the clarification "
                        + "response cannot publish the same "
                        + "response.",
                exception.getMessage()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .APPROVED,
                assessment.getAssessmentStatus()
        );

        assertNull(assessment.getPublishedByUserId());
        assertNull(assessment.getPublishedAt());

        verifyNoInteractions(supportTicketService);

        verify(assessmentRepository, never())
                .saveAndFlush(
                        any(
                                SupportTicketAiAssessment.class
                        )
                );

        verifyNoInteractions(auditService);
    }

    @Test
    void publishApprovedResponse_shouldPublishForDifferentUser() {
        SupportTicketAiAssessment assessment =
                approvedAssessment(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .APPROVED
                );

        SupportTicket publishedTicket =
                clarificationTicket();

        publishedTicket.setStatus(
                SupportTicket.TicketStatus.RESPONDED
        );

        publishedTicket.setResponse(
                assessment.getApprovedResponse()
        );

        publishedTicket.setRespondedByUserId(
                PUBLISHER_USER_ID
        );

        publishedTicket.setRespondedAt(
                LocalDateTime.now()
        );

        publishedTicket.setPublicClarification(true);

        SupportTicketResponse publishedResponse =
                SupportTicketResponse.from(
                        publishedTicket
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(PUBLISHER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        when(supportTicketService
                .publishOfficialClarification(
                        TICKET_ID,
                        assessment.getApprovedResponse()
                )
        ).thenReturn(publishedResponse);

        when(assessmentRepository.saveAndFlush(
                assessment
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        SupportTicketAiAssessmentResponse response =
                service.publishApprovedResponse(
                        ASSESSMENT_ID
                );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .RESPONSE_PUBLISHED,
                assessment.getAssessmentStatus()
        );

        assertEquals(
                PUBLISHER_USER_ID,
                assessment.getPublishedByUserId()
        );

        assertNotNull(assessment.getPublishedAt());

        assertEquals(
                APPROVER_USER_ID,
                assessment.getApprovedByUserId()
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

        verify(supportTicketService)
                .publishOfficialClarification(
                        TICKET_ID,
                        assessment.getApprovedResponse()
                );

        verify(assessmentRepository)
                .saveAndFlush(assessment);

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .SUPPORT_TICKET_AI_RESPONSE_PUBLISHED
                        ),
                        eq(TENDER_ID),
                        isNull(),
                        eq(PUBLISHER_USER_ID),
                        eq(
                                "Approved AI ticket "
                                        + "response published"
                        ),
                        argThat(details ->
                                details.contains(
                                        "approvedByUserId="
                                                + APPROVER_USER_ID
                                )
                                        && details.contains(
                                        "publishedByUserId="
                                                + PUBLISHER_USER_ID
                                )
                                        && details.contains(
                                        "publicClarification=true"
                                )
                        )
                );
    }

    @Test
    void publishApprovedResponse_shouldRejectInvalidAssessmentState() {
        SupportTicketAiAssessment assessment =
                approvedAssessment(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .REJECTED
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(PUBLISHER_USER_ID);

        when(assessmentRepository.findById(
                ASSESSMENT_ID
        )).thenReturn(Optional.of(assessment));

        TicketAiAssessmentStateException exception =
                assertThrows(
                        TicketAiAssessmentStateException.class,
                        () -> service.publishApprovedResponse(
                                ASSESSMENT_ID
                        )
                );

        assertEquals(
                "Only APPROVED or EDITED_AND_APPROVED "
                        + "assessments can be published.",
                exception.getMessage()
        );

        assertEquals(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .REJECTED,
                assessment.getAssessmentStatus()
        );

        assertNull(assessment.getPublishedByUserId());
        assertNull(assessment.getPublishedAt());

        verifyNoInteractions(supportTicketService);

        verify(assessmentRepository, never())
                .saveAndFlush(
                        any(
                                SupportTicketAiAssessment.class
                        )
                );

        verifyNoInteractions(auditService);
    }

    private SupportTicketAiAssessment generatedAssessment(
            SupportTicketAiAssessment.AiResolutionMode
                    resolutionMode
    ) {
        return SupportTicketAiAssessment.builder()
                .id(ASSESSMENT_ID)
                .assessmentReference(
                        "KB-AI-ASSESSMENT-000071"
                )
                .ticketId(TICKET_ID)
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
                .resolutionMode(resolutionMode)
                .assessmentStatus(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .GENERATED
                )
                .summary(
                        "The trader requests clarification "
                                + "on phased delivery."
                )
                .suggestedResponse(
                        "The tender permits phased delivery "
                                + "subject to the final deadline."
                )
                .confidenceScore(
                        new BigDecimal("0.94")
                )
                .modelVersion("test-model-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    private SupportTicketAiAssessment approvedAssessment(
            SupportTicketAiAssessment.AiAssessmentStatus
                    status
    ) {
        LocalDateTime approvedAt =
                LocalDateTime.now().minusMinutes(5);

        return SupportTicketAiAssessment.builder()
                .id(ASSESSMENT_ID)
                .assessmentReference(
                        "KB-AI-ASSESSMENT-000071"
                )
                .ticketId(TICKET_ID)
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
                .assessmentStatus(status)
                .summary(
                        "The trader requests clarification "
                                + "on phased delivery."
                )
                .suggestedResponse(
                        "The tender permits phased delivery "
                                + "subject to the final deadline."
                )
                .approvedResponse(
                        "The tender permits phased delivery "
                                + "subject to the final deadline."
                )
                .approvedByUserId(APPROVER_USER_ID)
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
                                + "the delivery question."
                )
                .confidenceScore(
                        new BigDecimal("0.94")
                )
                .modelVersion("test-model-1")
                .createdAt(
                        approvedAt.minusMinutes(5)
                )
                .updatedAt(approvedAt)
                .version(1L)
                .build();
    }

    private SupportTicket clarificationTicket() {
        return SupportTicket.builder()
                .id(TICKET_ID)
                .ticketReference(
                        "KB-TICKET-CLAR002"
                )
                .tenderId(TENDER_ID)
                .bidId(null)
                .createdByUserId(8L)
                .ticketType(
                        SupportTicket.TicketType
                                .CLARIFICATION_REQUEST
                )
                .status(
                        SupportTicket.TicketStatus
                                .IN_REVIEW
                )
                .subject(
                        "Clarification on phased delivery"
                )
                .description(
                        "Can deliveries be completed "
                                + "in approved phases?"
                )
                .assignedToUserId(PUBLISHER_USER_ID)
                .reviewedByUserId(PUBLISHER_USER_ID)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ApproveTicketAiAssessmentRequest approvalRequest(
            String approvedResponse,
            boolean relevanceConfirmed,
            String relevanceNotes
    ) {
        ApproveTicketAiAssessmentRequest request =
                new ApproveTicketAiAssessmentRequest();

        request.setApprovedResponse(approvedResponse);
        request.setResponseRelevanceConfirmed(
                relevanceConfirmed
        );
        request.setResponseRelevanceConfirmationNotes(
                relevanceNotes
        );

        return request;
    }
}