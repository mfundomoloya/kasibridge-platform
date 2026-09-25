package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.event.SupportTicketCreatedEvent;
import com.kasibridge.procurement.exception.*;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService{

    private final SupportTicketRepository ticketRepository;
    private final TenderRepository tenderRepository;
    private final CurrentUserService currentUserService;
    private final TraderProfileClient traderProfileClient;
    private final ProcurementAuditService auditService;
    private final NotificationOutboxService notificationOutboxService;
    private final BidRepository bidRepository;
    private final WhatsAppMessageTemplateService whatsAppMessageTemplateService;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SupportTicketResponse createTicket(Long tenderId, CreateSupportTicketRequest request) {
        if(!tenderRepository.existsById(tenderId)){
            throw new TenderNotFoundException("Tender not found with ID: " +  tenderId);
        }

        Long actorUserId = currentUserService.getCurrentUserId();

        TraderProfileClientResponse trader = traderProfileClient.getTraderProfileByUserId(actorUserId);

        SupportTicket ticket = SupportTicket.builder()
                .ticketReference(generateTicketReference())
                .tenderId(tenderId)
                .bidId(request.getBidId())
                .createdByUserId(actorUserId)
                .traderId(trader.getId())
                .contactName(trader.getFullName())
                .contactPhoneNumber(trader.getPhoneNumber())
                .contactEmail(trader.getEmail())
                .businessNameSnapshot(trader.getBusinessName())
                .ticketType(request.getTicketType())
                .status(SupportTicket.TicketStatus.OPEN)
                .subject(request.getSubject().trim())
                .description(request.getDescription().trim())
                .publicClarification(false)
                .build();

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_CREATED,
                tenderId,
                request.getBidId(),
                actorUserId,
                "Support ticket created",
                "Ticket reference=" + saved.getTicketReference() + ", type=" + saved.getTicketType()
                        + ", traderId=" + saved.getTraderId()
        );

        queueTicketCreatedNotification(saved);

        eventPublisher.publishEvent(new SupportTicketCreatedEvent(saved.getId()));

        return SupportTicketResponse.from(saved);
    }

    @Override
    public Page<SupportTicketResponse> getTicketsForTender(Long tenderId, Pageable pageable) {
        if(!tenderRepository.existsById(tenderId)){
            throw new TenderNotFoundException("Tender not found with ID: " +  tenderId);
        }

        return ticketRepository.findByTenderId(tenderId, pageable).map(SupportTicketResponse::from);
    }

    @Override
    public Page<SupportTicketResponse> getMyTickets(Pageable pageable) {
        Long actorUserId = currentUserService.getCurrentUserId();

        return ticketRepository.findByCreatedByUserId(actorUserId, pageable).map(SupportTicketResponse::from);
    }

    @Override
    public SupportTicketResponse getTicketById(Long ticketId) {
        return SupportTicketResponse.from(findTicket(ticketId));
    }

    @Override
    @Transactional
    public SupportTicketResponse respondToTicket(Long ticketId, RespondToTicketRequest request) {

        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicketForUpdate(ticketId);

        assertCurrentUserCanHandleTicket(ticket, actorUserId);

        if (ticket.getTicketType()
                == SupportTicket.TicketType.CLARIFICATION_REQUEST) {
            throw new SupportTicketStateException(
                    "Clarification requests must be published through the official clarification approval workflow."
            );
        }

        if(ticket.getStatus() != SupportTicket.TicketStatus.IN_REVIEW){
            throw new SupportTicketStateException("Only IN_REVIEW tickets can be responded to.");
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setResponse(request.getResponse().trim());
        ticket.setRespondedByUserId(actorUserId);
        ticket.setRespondedAt(now);
        ticket.setUpdatedAt(now);
        //Ordinary support responses must never enter the public clarification feed.
        ticket.setPublicClarification(false);
        ticket.setStatus(SupportTicket.TicketStatus.RESPONDED);

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_RESPONDED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket responded",
                "Ticket reference=" + saved.getTicketReference()
                        + ", assignedToUserId="
                        + saved.getAssignedToUserId()
                        + ", reviewedByUserId="
                        + saved.getReviewedByUserId()
                        + ", publicClarification=false"
        );

        queueTicketRespondedNotification(saved);

        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketResponse closeTicket(Long ticketId, CloseTicketRequest request) {
        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicketForUpdate(ticketId);

        assertCurrentUserCanHandleTicket(ticket, actorUserId);

        if (ticket.getStatus() != SupportTicket.TicketStatus.RESPONDED) {
            throw new SupportTicketStateException("Only RESPONDED tickets can be closed.");
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(SupportTicket.TicketStatus.CLOSED);
        ticket.setClosedByUserId(actorUserId);
        ticket.setClosedAt(now);
        ticket.setClosureNotes(request.getClosureNotes().trim());
        ticket.setUpdatedAt(now);

        SupportTicket saved = ticketRepository.save(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_CLOSED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket closed",
                "Ticket reference=" + saved.getTicketReference()
                        + ", assignedToUserId="
                        + saved.getAssignedToUserId()
                        + ", respondedByUserId="
                        + saved.getRespondedByUserId()
                        + ", closureNotes="
                        + saved.getClosureNotes()
        );

        queueTicketClosedNotification(saved);
        return SupportTicketResponse.from(saved);
    }

    @Override
    public Page<SupportTicketResponse> getPublicClarifications(Long tenderId, Pageable pageable) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }

        return ticketRepository.findByTenderIdAndPublicClarificationTrue(tenderId, pageable)
                .map(SupportTicketResponse::from);
    }

    @Override
    @Transactional
    public SupportTicketResponse startReview(Long ticketId) {
        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicketForUpdate(ticketId);

        assertCurrentUserCanHandleTicket(ticket, actorUserId);

        if (ticket.getStatus()
                != SupportTicket.TicketStatus.OPEN) {

            throw new SupportTicketStateException("Only OPEN tickets can be moved into review.");
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(SupportTicket.TicketStatus.IN_REVIEW);

        ticket.setReviewedByUserId(actorUserId);
        ticket.setReviewStartedAt(now);
        ticket.setUpdatedAt(now);

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_REVIEW_STARTED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket review started",
                "Ticket reference="
                        + saved.getTicketReference()
        );

        log.info("Support ticket moved into review: ticketId={} reviewedByUserId={}", saved.getId(), actorUserId);

        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketResponse rejectTicket(Long ticketId, RejectSupportTicketRequest request) {
        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicketForUpdate(ticketId);

        assertCurrentUserCanHandleTicket(ticket, actorUserId);
        if (ticket.getStatus()
                != SupportTicket.TicketStatus.IN_REVIEW) {
            throw new SupportTicketStateException("Only IN_REVIEW tickets can be rejected.");
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(SupportTicket.TicketStatus.REJECTED);

        ticket.setRejectedByUserId(actorUserId);
        ticket.setRejectedAt(now);
        ticket.setRejectionReason(
                request.getRejectionReason().trim()
        );
        ticket.setUpdatedAt(now);

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_REJECTED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket rejected",
                "Ticket reference="
                        + saved.getTicketReference()
                        + ", reason="
                        + saved.getRejectionReason()
        );

        log.info(
                "Support ticket rejected: ticketId={} rejectedByUserId={}",
                saved.getId(),
                actorUserId
        );

        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketResponse assignTicket(Long ticketId, AssignSupportTicketRequest request) {

        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicketForUpdate(ticketId);

        Long requestedAssignedUserId = request.getAssignedToUserId();
        Long existingAssignedUserId = ticket.getAssignedToUserId();

        boolean reassignment = existingAssignedUserId != null;

        assertTicketCanBeAssigned(
                ticket,
                actorUserId,
                requestedAssignedUserId,
                reassignment
        );

        if (reassignment && ticket.getStatus() == SupportTicket.TicketStatus.IN_REVIEW) {

            auditBlockedTicketReassignment(
                    ticket,
                    actorUserId,
                    requestedAssignedUserId
            );

            throw new SupportTicketStateException("Ticket cannot be reassigned while it is under active review. " +
                    "Return the ticket to the assignment queue before reassigning it.");
        }

        //prevent assigning the ticket to its current assignee
        if (Objects.equals(
                existingAssignedUserId,
                requestedAssignedUserId
        )) {
            auditRejectedTicketAssignment(
                    ticket,
                    actorUserId,
                    requestedAssignedUserId,
                    true,
                    "Ticket is already assigned to the requested user"
            );
            throw new SupportTicketStateException(
                    "Ticket is already assigned to user ID: "
                            + requestedAssignedUserId
            );
        }

        OffsetDateTime assignmentTime = OffsetDateTime.now();

        LocalDateTime now = LocalDateTime.now();

        ticket.setAssignedToUserId(request.getAssignedToUserId());
        ticket.setAssignedByUserId(actorUserId);
        ticket.setAssignedAt(assignmentTime);
        ticket.setAssignmentReason(request.getAssignmentReason().trim());
        ticket.setAssignmentSource(SupportTicket.AssignmentSource.MANUAL);
        ticket.setUpdatedAt(now);

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        ProcurementAuditEvent.AuditEventType auditEventType = reassignment
                        ? ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_REASSIGNED
                        : ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_ASSIGNED;

        auditService.recordSuccess(
                auditEventType,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                reassignment
                        ? "Support ticket reassigned"
                        : "Support ticket assigned",
                "Ticket reference="
                        + saved.getTicketReference()
                        + ", assignedToUserId="
                        + saved.getAssignedToUserId()
                        + ", previousAssignedToUserId="
                        + existingAssignedUserId
                        + ", source="
                        + saved.getAssignmentSource()
                        + ", reason="
                        + saved.getAssignmentReason()
        );

        log.info(
                "Support ticket assignment updated: ticketId={} assignedToUserId={} assignedByUserId={} reassignment={}",
                saved.getId(),
                saved.getAssignedToUserId(),
                actorUserId,
                reassignment
        );

        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyAssignedTickets(Pageable pageable) {
        Long actorUserId = currentUserService.getCurrentUserId();

        return ticketRepository.findByAssignedToUserId(actorUserId, pageable)
                .map(SupportTicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyAssignedTicketsByStatus(SupportTicket.TicketStatus status, Pageable pageable) {
        Long actorUserId = currentUserService.getCurrentUserId();

        return ticketRepository.findByAssignedToUserIdAndStatus(
                        actorUserId,
                        status,
                        pageable
                )
                .map(SupportTicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getUnassignedTickets(Pageable pageable) {
        return ticketRepository.findByAssignedToUserIdIsNull(pageable)
                .map(SupportTicketResponse::from);
    }

    @Override
    @Transactional
    public SupportTicketResponse returnTicketToQueue(Long ticketId, ReturnSupportTicketToQueueRequest request) {
        Long actorUserId = currentUserService.getCurrentUserId();

        /*
         * Acquire the same pessimistic write lock used by
         * assignTicket() and startReview().
         */
        SupportTicket ticket =
                findTicketForUpdate(ticketId);

        /*
         * Only the current assignee or a platform administrator
         * may return the ticket to the queue.
         */
        assertCurrentUserCanHandleTicket(ticket, actorUserId);

        /*
         * Only an actively reviewed ticket may be returned.
         */
        if (ticket.getStatus() != SupportTicket.TicketStatus.IN_REVIEW) {

            throw new SupportTicketStateException("Only IN_REVIEW tickets can be returned to the assignment queue.");
        }

        Long previousAssignedToUserId = ticket.getAssignedToUserId();

        Long previousAssignedByUserId = ticket.getAssignedByUserId();

        Long previousReviewedByUserId = ticket.getReviewedByUserId();

        OffsetDateTime previousAssignedAt = ticket.getAssignedAt();

        LocalDateTime previousReviewStartedAt = ticket.getReviewStartedAt();

        SupportTicket.TicketStatus previousStatus = ticket.getStatus();

        LocalDateTime now = LocalDateTime.now();

        /*
         * Reset the active workflow to an OPEN, unassigned state.
         *
         * Historical values are preserved in the audit event below.
         */
        ticket.setStatus(SupportTicket.TicketStatus.OPEN);

        ticket.setAssignedToUserId(null);
        ticket.setAssignedByUserId(null);
        ticket.setAssignedAt(null);
        ticket.setAssignmentReason(null);
        ticket.setAssignmentSource(null);

        ticket.setReviewedByUserId(null);
        ticket.setReviewStartedAt(null);

        ticket.setUpdatedAt(now);

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_RETURNED_TO_QUEUE,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket returned to assignment queue",
                "Ticket reference="
                        + saved.getTicketReference()
                        + ", previousStatus="
                        + previousStatus
                        + ", newStatus="
                        + saved.getStatus()
                        + ", previousAssignedToUserId="
                        + previousAssignedToUserId
                        + ", previousAssignedByUserId="
                        + previousAssignedByUserId
                        + ", previousAssignedAt="
                        + previousAssignedAt
                        + ", previousReviewedByUserId="
                        + previousReviewedByUserId
                        + ", previousReviewStartedAt="
                        + previousReviewStartedAt
                        + ", reason="
                        + request.getReason().trim()
        );

        log.info(
                "Support ticket returned to assignment queue: "
                        + "ticketId={} previousAssignedToUserId={} "
                        + "previousReviewedByUserId={} actorUserId={}",
                saved.getId(),
                previousAssignedToUserId,
                previousReviewedByUserId,
                actorUserId
        );

        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketResponse publishOfficialClarification(Long ticketId, String approvedResponse) {

        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicketForUpdate(ticketId);

        assertCurrentUserCanHandleTicket(ticket, actorUserId);

        if (ticket.getTicketType() != SupportTicket.TicketType.CLARIFICATION_REQUEST) {

            throw new SupportTicketStateException("Only CLARIFICATION_REQUEST tickets can be published as official clarifications.");
        }

        if (ticket.getStatus() != SupportTicket.TicketStatus.IN_REVIEW) {

            throw new SupportTicketStateException("Only IN_REVIEW clarification requests can be published.");
        }

        if (approvedResponse == null || approvedResponse.isBlank()) {

            throw new SupportTicketStateException("An approved clarification response is required.");
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setResponse(approvedResponse.trim());

        ticket.setRespondedByUserId(actorUserId);
        ticket.setRespondedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setPublicClarification(true);

        ticket.setStatus(SupportTicket.TicketStatus.RESPONDED);

        SupportTicket saved = ticketRepository.saveAndFlush(ticket);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_RESPONDED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket responded",
                "Ticket reference="
                        + saved.getTicketReference()
                        + ", ticketType="
                        + saved.getTicketType()
                        + ", assignedToUserId="
                        + saved.getAssignedToUserId()
                        + ", reviewedByUserId="
                        + saved.getReviewedByUserId()
        );

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType
                        .OFFICIAL_CLARIFICATION_PUBLISHED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Official clarification published",
                "Ticket reference="
                        + saved.getTicketReference()
                        + ", publicClarification=true"
        );

        queueTicketRespondedNotification(saved);
        queueOfficialClarificationBroadcast(saved);

        return SupportTicketResponse.from(saved);
    }

    private SupportTicket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketException(
                        "Support ticket not found with ID: " + ticketId
                ));
    }

    private SupportTicket findTicketForUpdate(Long ticketId) {
        return ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new SupportTicketException("Support ticket not found with ID: " + ticketId)
                );
    }

    private String generateTicketReference() {
        return "KB-TICKET-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private void queueTicketCreatedNotification(SupportTicket ticket) {

        boolean alreadyQueued = notificationOutboxRepository.existsByTemplateTypeAndRelatedTicketIdAndRecipientUserId(
                        NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_CREATED,
                        ticket.getId(),
                        ticket.getCreatedByUserId()
                );

        if (alreadyQueued) {
            log.warn("Skipping duplicate SUPPORT_TICKET_CREATED notification for ticketId={} recipientUserId={}",
                    ticket.getId(),
                    ticket.getCreatedByUserId()
            );
            return;
        }

        WhatsAppTemplateContext context = WhatsAppTemplateContext.builder()

                .recipientName(ticket.getContactName())
                .tenderId(ticket.getTenderId())
                .ticketId(ticket.getId())
                .ticketReference(ticket.getTicketReference())
                .ticketType(ticket.getTicketType().name())
                .ticketSubject(ticket.getSubject())
                .build();

        String message = whatsAppMessageTemplateService.generateMessage(
                NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_CREATED,
                context
        );

        notificationOutboxService.queueNotification(
                NotificationOutbox.NotificationChannel.WHATSAPP,
                NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_CREATED,
                ticket.getCreatedByUserId(),
                ticket.getContactPhoneNumber(),
                ticket.getContactEmail(),
                message,
                ticket.getTenderId(),
                ticket.getBidId(),
                ticket.getId()
        );

    }

    private void queueOfficialClarificationNotification(SupportTicket ticket) {
        String message = "Official clarification published for tender ID "
                + ticket.getTenderId()
                + ". Ticket reference: "
                + ticket.getTicketReference()
                + ". All bidders can now view the same response.";

        notificationOutboxService.queueNotification(
                NotificationOutbox.NotificationChannel.WHATSAPP,
                NotificationOutbox.NotificationTemplateType.OFFICIAL_CLARIFICATION_PUBLISHED,
                ticket.getCreatedByUserId(),
                ticket.getContactPhoneNumber(),
                ticket.getContactEmail(),
                message,
                ticket.getTenderId(),
                ticket.getBidId(),
                ticket.getId()
        );
    }

    private void queueTicketClosedNotification(SupportTicket ticket) {
        WhatsAppTemplateContext context = WhatsAppTemplateContext.builder()

                .recipientName(ticket.getContactName())
                .tenderId(ticket.getTenderId())
                .ticketId(ticket.getId())
                .ticketReference(ticket.getTicketReference())
                .ticketType(ticket.getTicketType().name())
                .closureNotes(ticket.getClosureNotes())
                .build();

        String message = whatsAppMessageTemplateService.generateMessage(
                NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_CLOSED,
                context
        );

        notificationOutboxService.queueNotification(
                NotificationOutbox.NotificationChannel.WHATSAPP,
                NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_CLOSED,
                ticket.getCreatedByUserId(),
                ticket.getContactPhoneNumber(),
                ticket.getContactEmail(),
                message,
                ticket.getTenderId(),
                ticket.getBidId(),
                ticket.getId()
        );
    }

    private void queueOfficialClarificationBroadcast(SupportTicket ticket) {
        List<Bid> bids = bidRepository.findByTenderId(ticket.getTenderId());

        Set<Long> notifiedUserIds = new HashSet<>();

        for (Bid bid : bids) {
            try {
                TraderProfileClientResponse trader = traderProfileClient.getTraderProfileByIdAsSystem(bid.getTraderProfileId());

                if (trader.getUserId() == null) {
                    log.warn(
                            "Skipping clarification notification for traderId={} because userId is null",
                            trader.getId()
                    );
                    continue;
                }

                if (!notifiedUserIds.add(trader.getUserId())) {
                    continue;
                }

                WhatsAppTemplateContext context = WhatsAppTemplateContext.builder()

                        .recipientName(trader.getFullName())
                        .tenderId(ticket.getTenderId())
                        .ticketId(ticket.getId())
                        .ticketReference(ticket.getTicketReference())
                        .ticketSubject(ticket.getSubject())
                        .ticketResponse(ticket.getResponse())
                        .build();

                String message = whatsAppMessageTemplateService.generateMessage(
                        NotificationOutbox.NotificationTemplateType.OFFICIAL_CLARIFICATION_PUBLISHED,
                        context
                );

                notificationOutboxService.queueNotification(
                        NotificationOutbox.NotificationChannel.WHATSAPP,
                        NotificationOutbox.NotificationTemplateType.OFFICIAL_CLARIFICATION_PUBLISHED,
                        trader.getUserId(),
                        trader.getPhoneNumber(),
                        trader.getEmail(),
                        message,
                        ticket.getTenderId(),
                        bid.getId(),
                        ticket.getId()
                );

            } catch (Exception ex) {
                log.error(
                        "Failed to queue official clarification notification for bidId={} traderProfileId={}",
                        bid.getId(),
                        bid.getTraderProfileId(),
                        ex
                );
            }
        }

        log.info(
                "Official clarification broadcast queued for tenderId={} ticketId={} recipients={}",
                ticket.getTenderId(),
                ticket.getId(),
                notifiedUserIds.size()
        );
    }

    private void queueTicketRespondedNotification(SupportTicket ticket) {
        WhatsAppTemplateContext context = WhatsAppTemplateContext.builder()
                .recipientName(ticket.getContactName())
                .tenderId(ticket.getTenderId())
                .ticketId(ticket.getId())
                .ticketReference(ticket.getTicketReference())
                .ticketType(ticket.getTicketType().name())
                .ticketSubject(ticket.getSubject())
                .ticketResponse(ticket.getResponse())
                .build();

        String message = whatsAppMessageTemplateService.generateMessage(
                NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_RESPONDED,
                context
        );

        notificationOutboxService.queueNotification(
                NotificationOutbox.NotificationChannel.WHATSAPP,
                NotificationOutbox.NotificationTemplateType.SUPPORT_TICKET_RESPONDED,
                ticket.getCreatedByUserId(),
                ticket.getContactPhoneNumber(),
                ticket.getContactEmail(),
                message,
                ticket.getTenderId(),
                ticket.getBidId(),
                ticket.getId()
        );
    }

    private void assertTicketCanBeAssigned(SupportTicket ticket,
                                           Long actorUserId,
                                           Long requestedAssignedUserId,
                                           boolean reassignment) {

        String rejectionReason = switch (ticket.getStatus()) {
            case CLOSED ->
                    "Closed tickets cannot be assigned or reassigned.";
            case REJECTED ->
                    "Rejected tickets cannot be assigned or reassigned.";
            case RESPONDED ->
                    "Responded tickets cannot be assigned or reassigned.";
            default -> null;
        };

        if (rejectionReason == null) {
            return;
        }

        auditRejectedTicketAssignment(
                ticket,
                actorUserId,
                requestedAssignedUserId,
                reassignment,
                rejectionReason
        );

        throw new SupportTicketStateException(rejectionReason);
    }

    private void assertCurrentUserCanHandleTicket(SupportTicket ticket, Long actorUserId) {
        boolean platformAdmin =
                currentUserService.hasRole("ROLE_PLATFORM_ADMIN");

        if (platformAdmin) {
            return;
        }

        if (ticket.getAssignedToUserId() == null) {
            throw new SupportTicketAssignmentException("Ticket must be assigned before it can be handled.");
        }

        if (!ticket.getAssignedToUserId()
                .equals(actorUserId)) {
            throw new ProcurementAuthorizationException("User is not assigned to handle this ticket.");
        }
    }

    private void auditBlockedTicketReassignment(SupportTicket ticket, Long actorUserId, Long requestedAssignedUserId) {
        auditService.recordRejected(
                ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_REASSIGNMENT_BLOCKED,
                ticket.getTenderId(),
                ticket.getBidId(),
                actorUserId,
                "Support ticket reassignment blocked",
                "Ticket reference="
                        + ticket.getTicketReference()
                        + ", status="
                        + ticket.getStatus()
                        + ", currentAssignedToUserId="
                        + ticket.getAssignedToUserId()
                        + ", reviewedByUserId="
                        + ticket.getReviewedByUserId()
                        + ", requestedAssignedToUserId="
                        + requestedAssignedUserId
                        + ", reason=Ticket is under active review"
        );
    }

    private void auditRejectedTicketAssignment(SupportTicket ticket,Long actorUserId,Long requestedAssignedUserId,
                                               boolean reassignment,
                                               String rejectionReason
    ) {
        ProcurementAuditEvent.AuditEventType eventType =
                reassignment
                        ? ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_REASSIGNMENT_BLOCKED
                        : ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_ASSIGNMENT_REJECTED;

        auditService.recordRejected(
                eventType,
                ticket.getTenderId(),
                ticket.getBidId(),
                actorUserId,
                reassignment
                        ? "Support ticket reassignment blocked"
                        : "Support ticket assignment rejected",
                "Ticket reference="
                        + ticket.getTicketReference()
                        + ", ticketId="
                        + ticket.getId()
                        + ", status="
                        + ticket.getStatus()
                        + ", currentAssignedToUserId="
                        + ticket.getAssignedToUserId()
                        + ", requestedAssignedToUserId="
                        + requestedAssignedUserId
                        + ", reason="
                        + rejectionReason
        );
    }
}