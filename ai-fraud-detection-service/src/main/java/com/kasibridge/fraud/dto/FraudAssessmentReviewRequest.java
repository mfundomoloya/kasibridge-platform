package com.kasibridge.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record FraudAssessmentReviewRequest(

        @NotBlank(message = "Review notes are required.")
        @Size(min = 5, max = 1000, message = "Review notes must contain between 5 and 1000 characters.")
        String reviewNotes,

        @NotNull(message = "Expected assessment version is required.")
        @PositiveOrZero(message = "Expected assessment version cannot be negative.")
        Long expectedVersion
) {

    public FraudAssessmentReviewRequest {
        if (reviewNotes != null) {
            reviewNotes = reviewNotes.trim();
        }
    }
}