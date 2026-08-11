package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RespondToTicketRequest {

    @NotBlank(message = "Response is required")
    @Size(max = 3000, message = "Response cannot exceed 3000 characters")
    private String response;

    private boolean publicClarification;
}
