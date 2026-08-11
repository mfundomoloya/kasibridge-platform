package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CloseTicketRequest {

    @NotBlank(message = "Closure notes are required")
    @Size(max = 1000, message = "Closure notes cannot exceed 1000 characters")
    private String closureNotes;
}
