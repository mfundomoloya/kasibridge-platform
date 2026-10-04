package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.SupportTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSupportTicketRequest {

    private Long bidId;

    @NotNull(message = "Ticket type is required")
    private SupportTicket.TicketType ticketType;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(max = 3000, message = "Description cannot exceed 3000 characters")
    private String description;
}
