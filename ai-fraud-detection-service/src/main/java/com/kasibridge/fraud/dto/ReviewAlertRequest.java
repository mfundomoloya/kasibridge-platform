package com.kasibridge.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewAlertRequest {

    @NotBlank(message = "Reviewer name is required")
    private String reviewedBy;

    @Size(max = 500, message = "Review notes cannot exceed 500 characters")
    private String reviewNotes;

}
