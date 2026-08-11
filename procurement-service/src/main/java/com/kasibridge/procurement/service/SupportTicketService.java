package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.CloseTicketRequest;
import com.kasibridge.procurement.dto.CreateSupportTicketRequest;
import com.kasibridge.procurement.dto.RespondToTicketRequest;
import com.kasibridge.procurement.dto.SupportTicketResponse;
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
}
