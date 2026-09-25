package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveTicketAiAssessmentRequest {
    @Size(max = 4000, message = "Approved response cannot exceed 4000 characters")
    private String approvedResponse;

    @NotNull(message = "Response relevance confirmation is required")
    private Boolean responseRelevanceConfirmed;

    @NotBlank(message = "Response relevance confirmation notes are required")
    @Size(max = 1000, message = "Response relevance confirmation notes cannot exceed 1000 characters")
    private String responseRelevanceConfirmationNotes;
}
