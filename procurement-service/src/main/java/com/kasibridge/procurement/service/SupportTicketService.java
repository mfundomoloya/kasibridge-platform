package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;
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
}
