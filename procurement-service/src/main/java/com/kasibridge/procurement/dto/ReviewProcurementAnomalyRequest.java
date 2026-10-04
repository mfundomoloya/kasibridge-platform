package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewProcurementAnomalyRequest {

    @NotBlank(message = "Review notes are required")
    @Size(max = 1000, message = "Review notes cannot exceed 1000 characters")
    private String reviewNotes;
}