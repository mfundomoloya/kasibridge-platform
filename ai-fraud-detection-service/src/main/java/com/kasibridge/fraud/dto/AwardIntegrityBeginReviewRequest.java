package com.kasibridge.fraud.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AwardIntegrityBeginReviewRequest(

        @NotNull(message = "Expected version is required.")
        @PositiveOrZero(message = "Expected version cannot be negative.")
        Long expectedVersion
) {
}