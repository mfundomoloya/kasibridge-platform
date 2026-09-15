package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveTicketAiAssessmentRequest {
    @Size(
            max = 4000,
            message = "Approved response cannot exceed 4000 characters")
    private String approvedResponse;
}
