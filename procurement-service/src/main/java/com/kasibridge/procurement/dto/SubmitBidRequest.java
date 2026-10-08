package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitBidRequest {

    @NotBlank(message = "Technical proposal is required")
    @Size(max = 10000, message = "Technical proposal cannot exceed 10000 characters")
    private String technicalProposal;


    @NotNull(message = "Price amount is required")
    @DecimalMin(value = "0.01", message = "Price amount must be greater than 0")
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Price amount can contain up to 13 whole-number digits and 2 decimal places")
    private BigDecimal priceAmount;

    /*
    * Mocked compliance values.
    * In production, these would come from trusted compliance sources.
 *//*
     * DEVELOPMENT-ONLY COMPLIANCE DECLARATIONS.
     *
     * These values are supplied by the bidder and are not independently
     * verified. They must not be treated as authoritative compliance
     * results in production.
     *
     * Replace these fields with trusted compliance records or verified
     * service integrations before production deployment.
     */

    private boolean csdValid;
    private boolean taxClearanceValid;
    private boolean bbbeeValid;
    private boolean requiredDocumentsUploaded;
}
