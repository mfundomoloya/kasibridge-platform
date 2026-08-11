package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.SupportTicket;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportTicketResponse {
    private Long id;
    private String ticketReference;
    private Long tenderId;
    private Long bidId;
    private Long createdByUserId;
    private Long traderId;
    private String contactName;
    private String contactPhoneNumber;
    private String contactEmail;
    private String businessNameSnapshot;

    private SupportTicket.TicketType ticketType;
    private SupportTicket.TicketStatus status;

    private String subject;
    private String description;
    private String response;

    private Long respondedByUserId;
    private LocalDateTime respondedAt;

    private Long closedByUserId;
    private LocalDateTime closedAt;
    private String closureNotes;

    private boolean publicClarification;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupportTicketResponse from(SupportTicket ticket) {
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .ticketReference(ticket.getTicketReference())
                .tenderId(ticket.getTenderId())
                .bidId(ticket.getBidId())
                .createdByUserId(ticket.getCreatedByUserId())
                .traderId(ticket.getTraderId())
                .contactName(ticket.getContactName())
                .contactPhoneNumber(ticket.getContactPhoneNumber())
                .contactEmail(ticket.getContactEmail())
                .businessNameSnapshot(ticket.getBusinessNameSnapshot())
                .ticketType(ticket.getTicketType())
                .status(ticket.getStatus())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .response(ticket.getResponse())
                .respondedByUserId(ticket.getRespondedByUserId())
                .respondedAt(ticket.getRespondedAt())
                .closedByUserId(ticket.getClosedByUserId())
                .closedAt(ticket.getClosedAt())
                .closureNotes(ticket.getClosureNotes())
                .publicClarification(ticket.isPublicClarification())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
