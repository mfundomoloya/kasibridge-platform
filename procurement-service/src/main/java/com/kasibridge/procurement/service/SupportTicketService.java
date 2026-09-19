package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;
import com.kasibridge.procurement.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {

    SupportTicketResponse createTicket(Long tenderId, CreateSupportTicketRequest request);

    Page<SupportTicketResponse> getTicketsForTender(Long tenderId, Pageable pageable);

    Page<SupportTicketResponse> getMyTickets(Pageable pageable);

    SupportTicketResponse getTicketById(Long ticketId);

    SupportTicketResponse respondToTicket(Long ticketId, RespondToTicketRequest request);

    SupportTicketResponse closeTicket(Long ticketId, CloseTicketRequest request);

    Page<SupportTicketResponse> getPublicClarifications(Long tenderId, Pageable pageable);

    SupportTicketResponse startReview(Long ticketId);

    SupportTicketResponse rejectTicket(Long ticketId, RejectSupportTicketRequest request);

    SupportTicketResponse assignTicket(Long ticketId, AssignSupportTicketRequest request);

    Page<SupportTicketResponse> getMyAssignedTickets(Pageable pageable);

    Page<SupportTicketResponse> getMyAssignedTicketsByStatus(SupportTicket.TicketStatus status, Pageable pageable);

    Page<SupportTicketResponse> getUnassignedTickets(Pageable pageable);

    SupportTicketResponse returnTicketToQueue(Long ticketId, ReturnSupportTicketToQueueRequest request);
}
