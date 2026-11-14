package com.kasibridge.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AwardIntegrityReviewRequest(

        @NotBlank(message = "Review notes are required.")
        @Size(min = 10, max = 1000, message = "Review notes must contain between 10 and 1000 characters.")
        String reviewNotes,

        @NotNull(message = "Expected version is required.")
        @PositiveOrZero(message = "Expected version cannot be negative.")
        Long expectedVersion
) {

    public AwardIntegrityReviewRequest {
        reviewNotes =
                reviewNotes == null
                        ? null
                        : reviewNotes.trim();
    }
}