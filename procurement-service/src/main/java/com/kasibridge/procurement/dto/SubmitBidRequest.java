package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitBidRequest {

    @NotNull(message = "Trader ID is required")
    private Long traderId;

    @NotBlank(message = "Technical proposal is required")
    private String technicalProposal;


    @NotNull(message = "Price amount is required")
    @DecimalMin(value = "0.01", message = "Price amount must be greater than zero")
    private BigDecimal priceAmount;

    /*
    * Mocked compliance values.
    * In production, these would come from trusted compliance sources.
 */

    private boolean csdValid;
    private boolean taxClearanceValid;
    private boolean bbbeeValid;
    private boolean requiredDocumentsUploaded;
}
