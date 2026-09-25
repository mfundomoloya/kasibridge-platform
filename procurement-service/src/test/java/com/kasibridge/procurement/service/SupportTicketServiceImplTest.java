package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AssignSupportTicketRequest;
import com.kasibridge.procurement.dto.RespondToTicketRequest;
import com.kasibridge.procurement.dto.ReturnSupportTicketToQueueRequest;
import com.kasibridge.procurement.dto.SupportTicketResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
import com.kasibridge.procurement.exception.SupportTicketStateException;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static javax.management.Query.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceImplTest {

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TraderProfileClient traderProfileClient;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private NotificationOutboxService notificationOutboxService;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private WhatsAppMessageTemplateService whatsAppMessageTemplateService;

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SupportTicketServiceImpl supportTicketService;

    @Test
    void assignTicket_shouldRejectReassignmentWhenTicketIsInReview() {
        Long ticketId = 29L;

        SupportTicket ticket = SupportTicket.builder()
                .ticketReference("KB-TICKET-B60C6936")
                .tenderId(3L)
                .status(SupportTicket.TicketStatus.IN_REVIEW)
                .assignedToUserId(5L)
                .assignedByUserId(2L)
                .reviewedByUserId(5L)
                .build();

        AssignSupportTicketRequest request =
                new AssignSupportTicketRequest();

        request.setAssignedToUserId(7L);
        request.setAssignmentReason(
                "Transfer to another support reviewer."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(2L);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        SupportTicketStateException exception =
                assertThrows(
                        SupportTicketStateException.class,
                        () -> supportTicketService.assignTicket(
                                ticketId,
                                request
                        )
                );

        assertEquals(
                "Ticket cannot be reassigned while it is under active review. "
                        + "Return the ticket to the assignment queue before reassigning it.",
                exception.getMessage()
        );

        assertEquals(
                Long.valueOf(5L),
                ticket.getAssignedToUserId()
        );

        assertEquals(
                Long.valueOf(5L),
                ticket.getReviewedByUserId()
        );

        assertEquals(
                SupportTicket.TicketStatus.IN_REVIEW,
                ticket.getStatus()
        );

        verify(ticketRepository, never())
                .save(any(SupportTicket.class));

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verify(auditService).recordRejected(
                eq(
                        ProcurementAuditEvent.AuditEventType
                                .SUPPORT_TICKET_REASSIGNMENT_BLOCKED
                ),
                eq(3L),
                isNull(),
                eq(2L),
                eq("Support ticket reassignment blocked"),
                argThat(details ->
                        details != null
                                && details.contains(
                                "currentAssignedToUserId=5"
                        )
                                && details.contains(
                                "reviewedByUserId=5"
                        )
                                && details.contains(
                                "requestedAssignedToUserId=7"
                        )
                                && details.contains(
                                "status=IN_REVIEW"
                        )
                )
        );
    }

    @Test
    void startReview_shouldUseLockedTicketLookup() {
        Long ticketId = 29L;
        Long assignedUserId = 5L;

        SupportTicket ticket = SupportTicket.builder()
                .ticketReference("KB-TICKET-9B036713")
                .tenderId(3L)
                .status(SupportTicket.TicketStatus.OPEN)
                .assignedToUserId(assignedUserId)
                .assignedByUserId(2L)
                .build();

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole("ROLE_PLATFORM_ADMIN"))
                .thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.saveAndFlush(
                any(SupportTicket.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        SupportTicketResponse response = supportTicketService.startReview(ticketId);

        assertEquals(
                SupportTicket.TicketStatus.IN_REVIEW,
                ticket.getStatus()
        );

        assertEquals(
                Long.valueOf(5L),
                ticket.getReviewedByUserId()
        );

        assertEquals(
                Long.valueOf(5L),
                ticket.getAssignedToUserId()
        );

        assertNotNull(ticket.getReviewStartedAt());
        assertNotNull(ticket.getUpdatedAt());

        verify(ticketRepository)
                .findByIdForUpdate(ticketId);

        verify(ticketRepository)
                .saveAndFlush(ticket);

        verify(ticketRepository, never())
                .findById(ticketId);
    }

    @Test
    void returnTicketToQueue_shouldReturnInReviewTicketToOpenQueue() {
        Long ticketId = 7L;
        Long assignedUserId = 14L;

        LocalDateTime reviewStartedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        19,
                        12,
                        15,
                        32
                );

        OffsetDateTime assignedAt =
                OffsetDateTime.parse(
                        "2026-09-19T10:15:17.083297Z"
                );

        SupportTicket ticket = SupportTicket.builder()
                .ticketReference("KB-TICKET-F14A6B13")
                .tenderId(3L)
                .status(SupportTicket.TicketStatus.IN_REVIEW)
                .assignedToUserId(assignedUserId)
                .assignedByUserId(2L)
                .assignedAt(assignedAt)
                .assignmentReason(
                        "Assigned to the tender specification officer."
                )
                .assignmentSource(
                        SupportTicket.AssignmentSource.MANUAL
                )
                .reviewedByUserId(assignedUserId)
                .reviewStartedAt(reviewStartedAt)
                .build();

        ReturnSupportTicketToQueueRequest request =
                new ReturnSupportTicketToQueueRequest();

        request.setReason(
                "The assigned reviewer is unavailable."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole("ROLE_PLATFORM_ADMIN"))
                .thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.saveAndFlush(
                any(SupportTicket.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        SupportTicketResponse response =
                supportTicketService.returnTicketToQueue(
                        ticketId,
                        request
                );

        assertEquals(
                SupportTicket.TicketStatus.OPEN,
                ticket.getStatus()
        );

        assertNull(ticket.getAssignedToUserId());
        assertNull(ticket.getAssignedByUserId());
        assertNull(ticket.getAssignedAt());
        assertNull(ticket.getAssignmentReason());
        assertNull(ticket.getAssignmentSource());
        assertNull(ticket.getReviewedByUserId());
        assertNull(ticket.getReviewStartedAt());

        assertNotNull(ticket.getUpdatedAt());
        assertNotNull(response);

        verify(ticketRepository)
                .findByIdForUpdate(ticketId);

        verify(ticketRepository)
                .saveAndFlush(ticket);

        verify(auditService).recordSuccess(
                eq(
                        ProcurementAuditEvent.AuditEventType
                                .SUPPORT_TICKET_RETURNED_TO_QUEUE
                ),
                eq(3L),
                ArgumentMatchers.<Long>isNull(),
                eq(assignedUserId),
                eq(
                        "Support ticket returned to assignment queue"
                ),
                argThat(details ->
                        details != null
                                && details.contains(
                                "previousAssignedToUserId=14"
                        )
                )
        );

    }

    @Test
    void returnTicketToQueue_shouldRejectTicketThatIsNotInReview() {
        Long ticketId = 7L;
        Long assignedUserId = 14L;

        SupportTicket ticket = SupportTicket.builder()
                .ticketReference("KB-TICKET-F14A6B13")
                .tenderId(3L)
                .status(SupportTicket.TicketStatus.OPEN)
                .assignedToUserId(assignedUserId)
                .assignedByUserId(2L)
                .build();

        ReturnSupportTicketToQueueRequest request =
                new ReturnSupportTicketToQueueRequest();

        request.setReason(
                "Return ticket for reassignment."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole("ROLE_PLATFORM_ADMIN"))
                .thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        SupportTicketStateException exception =
                assertThrows(
                        SupportTicketStateException.class,
                        () ->
                                supportTicketService
                                        .returnTicketToQueue(
                                                ticketId,
                                                request
                                        )
                );

        assertEquals(
                "Only IN_REVIEW tickets can be returned "
                        + "to the assignment queue.",
                exception.getMessage()
        );

        assertEquals(
                SupportTicket.TicketStatus.OPEN,
                ticket.getStatus()
        );

        assertEquals(
                Long.valueOf(14L),
                ticket.getAssignedToUserId()
        );

        verify(ticketRepository, never())
                .save(any(SupportTicket.class));

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verifyNoInteractions(auditService);
    }

    @Test
    void respondToTicket_shouldRejectOpenTicket() {
        Long ticketId = 31L;
        Long assignedUserId = 5L;

        SupportTicket ticket =
                SupportTicket.builder()
                        .ticketReference("KB-TICKET-OPEN001")
                        .tenderId(3L)
                        .ticketType(
                                SupportTicket.TicketType.GENERAL_SUPPORT
                        )
                        .status(
                                SupportTicket.TicketStatus.OPEN
                        )
                        .assignedToUserId(assignedUserId)
                        .build();

        RespondToTicketRequest request =
                new RespondToTicketRequest();

        request.setResponse(
                "The support request has been reviewed."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        SupportTicketStateException exception =
                assertThrows(
                        SupportTicketStateException.class,
                        () -> supportTicketService
                                .respondToTicket(
                                        ticketId,
                                        request
                                )
                );

        assertEquals(
                "Only IN_REVIEW tickets can be responded to.",
                exception.getMessage()
        );

        assertEquals(
                SupportTicket.TicketStatus.OPEN,
                ticket.getStatus()
        );

        assertNull(ticket.getResponse());

        verify(ticketRepository)
                .findByIdForUpdate(ticketId);

        verify(ticketRepository, never())
                .save(any(SupportTicket.class));

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verifyNoInteractions(auditService);
    }

    @Test
    void respondToTicket_shouldRejectUserWhoIsNotAssigned() {
        Long ticketId = 32L;

        SupportTicket ticket =
                SupportTicket.builder()
                        .ticketReference("KB-TICKET-ASSIGN01")
                        .tenderId(3L)
                        .ticketType(
                                SupportTicket.TicketType.GENERAL_SUPPORT
                        )
                        .status(
                                SupportTicket.TicketStatus.IN_REVIEW
                        )
                        .assignedToUserId(5L)
                        .reviewedByUserId(5L)
                        .build();

        RespondToTicketRequest request =
                new RespondToTicketRequest();

        request.setResponse(
                "Attempted response by another officer."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(7L);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        ProcurementAuthorizationException exception =
                assertThrows(
                        ProcurementAuthorizationException.class,
                        () -> supportTicketService
                                .respondToTicket(
                                        ticketId,
                                        request
                                )
                );

        assertEquals(
                "User is not assigned to handle this ticket.",
                exception.getMessage()
        );

        assertNull(ticket.getResponse());

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verifyNoInteractions(auditService);
    }

    @Test
    void respondToTicket_shouldAllowAssignedOfficerForInReviewTicket() {
        Long ticketId = 33L;
        Long assignedUserId = 5L;

        SupportTicket ticket =
                SupportTicket.builder()
                        .ticketReference("KB-TICKET-REVIEW01")
                        .tenderId(3L)
                        .ticketType(
                                SupportTicket.TicketType.GENERAL_SUPPORT
                        )
                        .status(
                                SupportTicket.TicketStatus.IN_REVIEW
                        )
                        .assignedToUserId(assignedUserId)
                        .reviewedByUserId(assignedUserId)
                        .build();

        RespondToTicketRequest request =
                new RespondToTicketRequest();

        request.setResponse(
                "The issue has been reviewed and resolved."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.saveAndFlush(
                any(SupportTicket.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        SupportTicketResponse response =
                supportTicketService.respondToTicket(
                        ticketId,
                        request
                );

        assertEquals(
                SupportTicket.TicketStatus.RESPONDED,
                ticket.getStatus()
        );

        assertEquals(
                "The issue has been reviewed and resolved.",
                ticket.getResponse()
        );

        assertEquals(
                Long.valueOf(assignedUserId),
                ticket.getRespondedByUserId()
        );

        assertNotNull(ticket.getRespondedAt());
        assertNotNull(ticket.getUpdatedAt());

        assertEquals(
                false,
                ticket.isPublicClarification()
        );

        assertNotNull(response);

        verify(ticketRepository)
                .findByIdForUpdate(ticketId);

        verify(ticketRepository)
                .saveAndFlush(ticket);

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent.AuditEventType
                                        .SUPPORT_TICKET_RESPONDED
                        ),
                        eq(3L),
                        ArgumentMatchers.<Long>isNull(),
                        eq(assignedUserId),
                        eq("Support ticket responded"),
                        argThat(details ->
                                details != null
                                        && details.contains(
                                        "assignedToUserId=5"
                                )
                                        && details.contains(
                                        "reviewedByUserId=5"
                                )
                        )
                );
    }

    @Test
    void respondToTicket_shouldRejectClarificationRequest() {
        Long ticketId = 40L;
        Long assignedUserId = 5L;

        SupportTicket ticket =
                SupportTicket.builder()
                        .ticketReference("KB-TICKET-CLAR001")
                        .tenderId(3L)
                        .ticketType(
                                SupportTicket.TicketType
                                        .CLARIFICATION_REQUEST
                        )
                        .status(
                                SupportTicket.TicketStatus.IN_REVIEW
                        )
                        .assignedToUserId(assignedUserId)
                        .reviewedByUserId(assignedUserId)
                        .build();

        RespondToTicketRequest request =
                new RespondToTicketRequest();

        request.setResponse(
                "Phased delivery is permitted."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        SupportTicketStateException exception =
                assertThrows(
                        SupportTicketStateException.class,
                        () -> supportTicketService.respondToTicket(
                                ticketId,
                                request
                        )
                );

        assertEquals(
                "Clarification requests must be published through "
                        + "the official clarification approval workflow.",
                exception.getMessage()
        );

        assertNull(ticket.getResponse());
        assertEquals(
                SupportTicket.TicketStatus.IN_REVIEW,
                ticket.getStatus()
        );

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verifyNoInteractions(auditService);
    }

    @Test
    void publishOfficialClarification_shouldRejectGeneralSupportTicket() {
        Long ticketId = 41L;
        Long assignedUserId = 5L;

        SupportTicket ticket =
                SupportTicket.builder()
                        .ticketReference("KB-TICKET-SUPPORT01")
                        .tenderId(3L)
                        .ticketType(
                                SupportTicket.TicketType.GENERAL_SUPPORT
                        )
                        .status(
                                SupportTicket.TicketStatus.IN_REVIEW
                        )
                        .assignedToUserId(assignedUserId)
                        .reviewedByUserId(assignedUserId)
                        .build();

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        SupportTicketStateException exception =
                assertThrows(
                        SupportTicketStateException.class,
                        () -> supportTicketService
                                .publishOfficialClarification(
                                        ticketId,
                                        "Approved response"
                                )
                );

        assertEquals(
                "Only CLARIFICATION_REQUEST tickets can be "
                        + "published as official clarifications.",
                exception.getMessage()
        );

        assertEquals(false, ticket.isPublicClarification());

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verifyNoInteractions(auditService);
    }

    @Test
    void publishOfficialClarification_shouldPublishInReviewClarification() {
        Long ticketId = 42L;
        Long assignedUserId = 5L;

        SupportTicket ticket =
                SupportTicket.builder()
                        .ticketReference("KB-TICKET-CLAR002")
                        .tenderId(3L)
                        .ticketType(
                                SupportTicket.TicketType
                                        .CLARIFICATION_REQUEST
                        )
                        .status(
                                SupportTicket.TicketStatus.IN_REVIEW
                        )
                        .assignedToUserId(assignedUserId)
                        .reviewedByUserId(assignedUserId)
                        .createdByUserId(8L)
                        .contactName("Test Trader")
                        .contactPhoneNumber("+27610000001")
                        .contactEmail("trader@example.test")
                        .build();

        when(currentUserService.getCurrentUserId())
                .thenReturn(assignedUserId);

        when(currentUserService.hasRole(
                "ROLE_PLATFORM_ADMIN"
        )).thenReturn(false);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.saveAndFlush(
                any(SupportTicket.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        when(bidRepository.findByTenderId(3L))
                .thenReturn(Collections.emptyList());

        SupportTicketResponse response =
                supportTicketService
                        .publishOfficialClarification(
                                ticketId,
                                "Phased delivery is permitted, subject "
                                        + "to the approved final deadline."
                        );

        assertEquals(
                SupportTicket.TicketStatus.RESPONDED,
                ticket.getStatus()
        );

        assertEquals(true, ticket.isPublicClarification());

        assertEquals(
                Long.valueOf(assignedUserId),
                ticket.getRespondedByUserId()
        );

        assertNotNull(ticket.getRespondedAt());
        assertNotNull(response);

        verify(ticketRepository)
                .findByIdForUpdate(ticketId);

        verify(ticketRepository)
                .saveAndFlush(ticket);

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent.AuditEventType
                                        .OFFICIAL_CLARIFICATION_PUBLISHED
                        ),
                        eq(3L),
                        ArgumentMatchers.<Long>isNull(),
                        eq(assignedUserId),
                        eq("Official clarification published"),
                        argThat(details ->
                                details != null
                                        && details.contains(
                                        "publicClarification=true"
                                )
                        )
                );
    }

    @Test
    void assignTicket_shouldAuditAssignmentRejected_whenUnassignedTicketIsClosed() {
        Long ticketId = 100L;
        Long tenderId = 200L;
        Long bidId = 300L;
        Long actorUserId = 400L;
        Long requestedAssignedUserId = 500L;

        AssignSupportTicketRequest request =
                mock(AssignSupportTicketRequest.class);

        SupportTicket ticket =
                mock(SupportTicket.class);

        when(currentUserService.getCurrentUserId())
                .thenReturn(actorUserId);

        when(request.getAssignedToUserId())
                .thenReturn(requestedAssignedUserId);

        when(ticketRepository.findByIdForUpdate(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticket.getId())
                .thenReturn(ticketId);

        when(ticket.getTenderId())
                .thenReturn(tenderId);

        when(ticket.getBidId())
                .thenReturn(bidId);

        when(ticket.getTicketReference())
                .thenReturn("KB-TICKET-TEST0001");

        when(ticket.getAssignedToUserId())
                .thenReturn(null);

        when(ticket.getStatus())
                .thenReturn(SupportTicket.TicketStatus.CLOSED);

        SupportTicketStateException exception =
                assertThrows(
                        SupportTicketStateException.class,
                        () -> supportTicketService.assignTicket(
                                ticketId,
                                request
                        )
                );

        assertEquals(
                "Closed tickets cannot be assigned or reassigned.",
                exception.getMessage()
        );

        verify(auditService).recordRejected(
                eq(
                        ProcurementAuditEvent.AuditEventType
                                .SUPPORT_TICKET_ASSIGNMENT_REJECTED
                ),
                eq(tenderId),
                eq(bidId),
                eq(actorUserId),
                eq("Support ticket assignment rejected"),
                argThat(details ->
                        details != null
                                && details.contains(
                                "Ticket reference=KB-TICKET-TEST0001"
                        )
                                && details.contains(
                                "ticketId=" + ticketId
                        )
                                && details.contains(
                                "status=CLOSED"
                        )
                                && details.contains(
                                "currentAssignedToUserId=null"
                        )
                                && details.contains(
                                "requestedAssignedToUserId="
                                        + requestedAssignedUserId
                        )
                                && details.contains(
                                "reason=Closed tickets cannot be assigned or reassigned."
                        )
                )
        );

        verify(ticketRepository, never())
                .saveAndFlush(any(SupportTicket.class));

        verifyNoInteractions(notificationOutboxService);
    }
}