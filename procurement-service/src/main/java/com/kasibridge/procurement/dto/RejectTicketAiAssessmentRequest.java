package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectTicketAiAssessmentRequest {
    @NotBlank(message = "Rejection reason is required")
    @Size(
            max = 1500,
            message = "Rejection reason cannot exceed 1500 characters"

            )
    private String rejectionReason;
}
