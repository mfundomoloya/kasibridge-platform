package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.exception.SupportTicketException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.SupportTicketRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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

        SupportTicket saved = ticketRepository.save(ticket);

        queueTicketCreatedNotification(saved);

        auditService.recordSuccess(

                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_CREATED,
                tenderId,
                request.getBidId(),
                actorUserId,
                "Support ticket created",
                "Ticket reference=" + saved.getTicketReference() + ", type=" + saved.getTicketType()
                + ", traderId=" + saved.getTraderId()
        );

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

        SupportTicket ticket = findTicket(ticketId);

        if(ticket.getStatus() == SupportTicket.TicketStatus.CLOSED || ticket.getStatus() == SupportTicket.TicketStatus.REJECTED){
            throw new SupportTicketException("Closed or rejected tickets cannot be responded to.");
        }

        boolean publicClarification = ticket.getTicketType() == SupportTicket.TicketType.CLARIFICATION_REQUEST ||
                request.isPublicClarification();

        LocalDateTime now = LocalDateTime.now();

        ticket.setResponse(request.getResponse());
        ticket.setRespondedByUserId(actorUserId);
        ticket.setRespondedAt(LocalDateTime.now());
        ticket.setUpdatedAt(now);
        ticket.setPublicClarification(publicClarification);
        ticket.setStatus(SupportTicket.TicketStatus.RESPONDED);

        SupportTicket saved = ticketRepository.save(ticket);

        queueTicketRespondedNotification(saved);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_RESPONDED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket responded",
                "Ticket reference=" + saved.getTicketReference()
        );
        if (publicClarification) {

            queueOfficialClarificationNotification(saved);

            auditService.recordSuccess(
                    ProcurementAuditEvent.AuditEventType.OFFICIAL_CLARIFICATION_PUBLISHED,
                    saved.getTenderId(),
                    saved.getBidId(),
                    actorUserId,
                    "Official clarification published",
                    "Ticket reference=" + saved.getTicketReference()
            );
        }

        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketResponse closeTicket(Long ticketId, CloseTicketRequest request) {
        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicket ticket = findTicket(ticketId);

        if (ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            throw new SupportTicketException("Ticket is already closed.");
        }

        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(SupportTicket.TicketStatus.CLOSED);
        ticket.setClosedByUserId(actorUserId);
        ticket.setClosedAt(LocalDateTime.now());
        ticket.setUpdatedAt(now);
        ticket.setClosureNotes(request.getClosureNotes().trim());

        SupportTicket saved = ticketRepository.save(ticket);

        queueTicketClosedNotification(saved);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_CLOSED,
                saved.getTenderId(),
                saved.getBidId(),
                actorUserId,
                "Support ticket closed",
                "Ticket reference=" + saved.getTicketReference()
        );

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

    private SupportTicket findTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketException(
                        "Support ticket not found with ID: " + ticketId
                ));
    }

    private String generateTicketReference() {
        return "KB-TICKET-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private void queueTicketCreatedNotification(SupportTicket ticket) {
        String message = "✅ Ticket created: "
                + ticket.getTicketReference()
                + ". We have received your "
                + ticket.getTicketType()
                + " request and will respond as soon as possible.";

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
        String message = "📢 Official clarification published for tender ID "
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
        String message = "✅ Ticket closed: "
                + ticket.getTicketReference()
                + ". Closure notes are available in your ticket history.";

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

    private void queueTicketRespondedNotification(SupportTicket ticket) {
        String message = "📩 Ticket update: "
                + ticket.getTicketReference()
                + " has been responded to. Please log in to view the official response.";

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
}